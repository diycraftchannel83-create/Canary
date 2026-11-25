const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_breeder_1";
const BOB_UID = "bob_team_2";
const EVE_UID = "eve_intruder_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 1000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("aviaries").get());
  await assertFails(unauthDb.collection("aviary_invites").doc("KNR482").get());
  await assertFails(
    unauthDb.collection("aviaries").doc("av_1").set({
      ownerId: ALICE_UID,
      name: "Fake",
      inviteCode: "KNR482",
      memberUids: [ALICE_UID],
      memberLabels: ["Alice"],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile isolation and strict schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      activeAviaryId: "av_alice",
      supplementReminderEnabled: true,
      supplementHour: 7,
      supplementMinute: 30,
      incubationReminderEnabled: true,
      healthAlertThreshold: 25,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: pastDate() })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      supplementHour: 8,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      supplementHour: 8,
      supplementMinute: 0,
      updatedAt: pastDate(),
    })
  );
});

test("Aviary creation, invite join, and subcollection Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const ts = pastDate(5000);

  // Alice creates aviary & invite
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").set({
      ownerId: ALICE_UID,
      name: "Kenari Emas Aviary",
      inviteCode: "KNR482",
      memberUids: [ALICE_UID],
      memberLabels: ["Alice"],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("aviary_invites").doc("KNR482").set({
      aviaryId: "av_alice",
      aviaryName: "Kenari Emas Aviary",
      ownerId: ALICE_UID,
      createdAt: ts,
    })
  );

  // Listing invites is always forbidden
  await assertFails(bobDb.collection("aviary_invites").get());

  // Unfiltered aviaries query fails; filtered succeeds
  await assertFails(aliceDb.collection("aviaries").get());
  await assertSucceeds(
    aliceDb.collection("aviaries").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Bob resolves invite code and joins aviary
  await assertSucceeds(bobDb.collection("aviary_invites").doc("KNR482").get());
  await assertSucceeds(
    bobDb.collection("aviaries").doc("av_alice").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberLabels: ["Alice", "Bob"],
      inviteCode: "KNR482",
      updatedAt: pastDate(1000),
    })
  );

  // Eve cannot join without invite or spoof another user's UID
  await assertFails(
    eveDb.collection("aviaries").doc("av_alice").update({
      memberUids: [ALICE_UID, BOB_UID, "fake_uid"],
      memberLabels: ["Alice", "Bob", "Fake"],
      inviteCode: "KNR482",
      updatedAt: pastDate(500),
    })
  );

  // Alice creates a canary (no @DocumentId `id` field, optional null fields omitted)
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").set({
      aviaryId: "av_alice",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      ringCode: "KNR-2026-01",
      name: "Surya Gold",
      sex: "MALE",
      breed: "F1 Yorkshire",
      featherColor: "Kuning Intensif",
      hatchDateEpochMs: 1700000000000,
      cageCode: "KDG-01",
      breedingStatus: "ACTIVE",
      healthStatus: "HEALTHY",
      healthScore: 95,
      notes: "Gacor panjang",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject ghost field / @DocumentId `id` field
  await assertFails(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_bad").set({
      id: "c_bad",
      aviaryId: "av_alice",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      ringCode: "KNR-2026-99",
      name: "Bad",
      sex: "MALE",
      breed: "AF",
      featherColor: "Kuning",
      hatchDateEpochMs: 1700000000000,
      cageCode: "KDG-99",
      breedingStatus: "ACTIVE",
      healthStatus: "HEALTHY",
      healthScore: 90,
      notes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob (team member) can query canaries with memberUids filter; Eve cannot
  await assertSucceeds(
    bobDb.collection("aviaries").doc("av_alice").collection("canaries")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    bobDb.collection("aviaries").doc("av_alice").collection("canaries").get()
  );
  await assertFails(
    eveDb.collection("aviaries").doc("av_alice").collection("canaries").get()
  );
  await assertFails(
    eveDb.collection("aviaries").doc("av_alice").collection("canaries")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    eveDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").get()
  );

  // Alice creates a pair, a clutch with 21-day incubation start, health log, and supplement
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("pairs").doc("p_1").set({
      aviaryId: "av_alice",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      cageCode: "KDG-01",
      sireId: "c_sire_1",
      sireName: "Surya Gold",
      sireRing: "KNR-2026-01",
      damId: "c_dam_1",
      damName: "Ratu Kuning",
      damRing: "KNR-2026-02",
      pairedDateEpochMs: 1730000000000,
      status: "ACTIVE",
      notes: "Jodohan perdana",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const incStart = 1732000000000;
  const expectedHatch = incStart + 21 * 86400000;
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("clutches").doc("cl_1").set({
      aviaryId: "av_alice",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      pairId: "p_1",
      sireId: "c_sire_1",
      damId: "c_dam_1",
      cageCode: "KDG-01",
      clutchNumber: 1,
      eggCount: 4,
      fertileCount: 3,
      hatchedCount: 0,
      fledgedCount: 0,
      incubationStartEpochMs: incStart,
      expectedHatchEpochMs: expectedHatch,
      status: "INCUBATING",
      notes: "Mengeram hari ke-1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob updates clutch to COMPLETED (terminal state), then subsequent edits are blocked
  await assertSucceeds(
    bobDb.collection("aviaries").doc("av_alice").collection("clutches").doc("cl_1").update({
      hatchedCount: 3,
      fledgedCount: 3,
      actualHatchEpochMs: expectedHatch,
      status: "COMPLETED",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("aviaries").doc("av_alice").collection("clutches").doc("cl_1").update({
      eggCount: 5,
      updatedAt: pastDate(100),
    })
  );

  // Health log & Supplement log
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("health_logs").doc("h_1").set({
      aviaryId: "av_alice",
      userId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      canaryId: "c_sire_1",
      canaryName: "Surya Gold",
      canaryRing: "KNR-2026-01",
      healthScore: 45,
      previousScore: 95,
      healthStatus: "CRITICAL",
      symptoms: "Bulu mengembang, lesu",
      actionTaken: "Karantina hangat",
      recordedDateEpochMs: 1732400000000,
      alertTriggered: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("aviaries").doc("av_alice").collection("supplement_logs").doc("s_1").set({
      aviaryId: "av_alice",
      userId: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      title: "Vitamin E Breeding",
      dosage: "3 tetes / 50ml",
      targetGroup: "Semua Indukan",
      timeLabel: "07:00",
      lastCompletedDate: "2026-11-24",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Farm settings update by owner or team member succeeds
  await assertSucceeds(
    bobDb.collection("aviaries").doc("av_alice").update({
      farmName: "Bintang Canary Farm",
      farmAddress: "Jl. Kenari No. 88, Bandung",
      farmPhone: "081234567890",
      updatedAt: pastDate(300),
    })
  );

  // Canary photo can be uploaded once, then cannot be edited or deleted
  const fakePhoto = "A".repeat(150);
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").update({
      photoBase64: fakePhoto,
      photoCapturedAtEpochMs: 1732450000000,
      updatedAt: pastDate(250),
    })
  );
  // Updating other fields including genealogy & production history while leaving photo untouched succeeds
  await assertSucceeds(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").update({
      cageCode: "KDG-02",
      lineageNotes: "Trah F1 YS x AF Juara",
      recordedClutchCount: 3,
      recordedEggCount: 12,
      recordedHatchedCount: 10,
      recordedFledgedCount: 9,
      productionHistoryNotes: "Produktif stabil 4 telur per periode",
      updatedAt: pastDate(200),
    })
  );
  // Editing uploaded photo fails
  await assertFails(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").update({
      photoBase64: "B".repeat(150),
      updatedAt: pastDate(150),
    })
  );
  // Deleting/clearing uploaded photo fails
  await assertFails(
    aliceDb.collection("aviaries").doc("av_alice").collection("canaries").doc("c_sire_1").update({
      photoBase64: "",
      updatedAt: pastDate(100),
    })
  );
});
