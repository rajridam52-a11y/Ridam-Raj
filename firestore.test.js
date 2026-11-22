const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write any user backup or subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Unauthorized",
      priority: "HIGH",
      category: "Work",
      completed: false,
      recurrence: "NONE",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can write and read backup summary and subcollection items mirroring Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      deviceName: "Pixel 9",
      totalItems: 3,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set({
      userId: ALICE_UID,
      title: "Study AI",
      notes: "Chapter 4",
      triggerAtMillis: Date.now() + 3600000,
      repeatType: "DAILY",
      category: "Study",
      completed: false,
      priority: "HIGH",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Submit report",
      priority: "MEDIUM",
      category: "Work",
      completed: false,
      recurrence: "NONE",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alarms").doc("alarm_1").set({
      userId: ALICE_UID,
      hour: 6,
      minute: 30,
      label: "Morning Run",
      repeatDaysMask: 62,
      enabled: true,
      vibrate: true,
      snoozeMinutes: 10,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("events").doc("evt_1").set({
      userId: ALICE_UID,
      title: "Design Review",
      startMillis: Date.now() + 7200000,
      endMillis: Date.now() + 10800000,
      category: "Appointments",
      allDay: false,
      recurrence: "NONE",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("birthdays").doc("bday_1").set({
      userId: ALICE_UID,
      name: "Alex",
      month: 12,
      day: 15,
      birthYear: 1998,
      notes: "Likes coffee",
      advanceDays: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Project Plan",
      content: "Architecture notes",
      category: "Work",
      pinned: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1").set({
      userId: ALICE_UID,
      title: "Read pages",
      targetCount: 30,
      currentCount: 10,
      unit: "pages",
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("hab_1").set({
      userId: ALICE_UID,
      name: "Hydrate",
      category: "Health",
      streak: 5,
      bestStreak: 12,
      historyDatesCsv: "2026-11-20,2026-11-21",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );
});

test("Rejects ghost fields, @DocumentId 'id' leakage, future timestamps, and immutable createdAt updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 10000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("bad_id").set({
      id: "bad_id",
      userId: ALICE_UID,
      title: "Has id field",
      priority: "LOW",
      category: "Personal",
      completed: false,
      recurrence: "NONE",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("future_ts").set({
      userId: ALICE_UID,
      title: "Future timestamp",
      priority: "LOW",
      category: "Personal",
      completed: false,
      recurrence: "NONE",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t_ok").set({
      userId: ALICE_UID,
      title: "Initial",
      priority: "LOW",
      category: "Personal",
      completed: false,
      recurrence: "NONE",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t_ok").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t_ok").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("notes").doc("secret_note").set({
      userId: ALICE_UID,
      title: "Private",
      content: "Secret",
      category: "Personal",
      pinned: false,
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("secret_note").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("secret_note").update({
      title: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
