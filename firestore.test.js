const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_noor_123";
const BOB_UID = "bob_noor_456";

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

test("Unauthenticated user cannot read or write profile, bookmarks, or plans", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profile").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      userId: ALICE_UID,
      surahNumber: 18,
      ayahNumber: 10,
      pageNumber: 294,
      label: "الكهف",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memorization_plans").get());
});

test("Authenticated owner: valid profile create and partial update succeed; ghost field & future timestamp fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").set({
      userId: ALICE_UID,
      displayName: "أحمد",
      lastSurah: 18,
      lastAyah: 1,
      lastPage: 293,
      khatmahCount: 1,
      tasbeehTotal: 330,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").update({
      lastSurah: 18,
      lastAyah: 45,
      lastPage: 299,
      tasbeehTotal: 400,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("ghost").set({
      userId: ALICE_UID,
      displayName: "أحمد",
      lastSurah: 1,
      lastAyah: 1,
      lastPage: 1,
      khatmahCount: 0,
      tasbeehTotal: 0,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").update({
      lastPage: 300,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: bookmarks CRUD & @DocumentId 'id' exclusion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_18_10").set({
      userId: ALICE_UID,
      surahNumber: 18,
      ayahNumber: 10,
      pageNumber: 294,
      label: "دعاء أهل الكهف",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  // Reject explicit id field (conflicts with @DocumentId)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_2").set({
      id: "bm_2",
      userId: ALICE_UID,
      surahNumber: 2,
      ayahNumber: 255,
      pageNumber: 42,
      label: "آية الكرسي",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Memorization plan: terminal COMPLETED lock & cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memorization_plans").doc("plan_67").set({
      userId: ALICE_UID,
      surahNumber: 67,
      surahName: "الملك",
      dailyAyahTarget: 5,
      completedAyahs: 10,
      totalAyahs: 30,
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read or write Alice's plan
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memorization_plans").doc("plan_67").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memorization_plans").doc("plan_67").update({
      completedAyahs: 20,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice marks plan COMPLETED
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memorization_plans").doc("plan_67").update({
      completedAyahs: 30,
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: subsequent update on COMPLETED plan fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memorization_plans").doc("plan_67").update({
      completedAyahs: 15,
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
