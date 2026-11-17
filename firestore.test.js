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

test("Unauthenticated user: cannot read users profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: Alice can create her own valid profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      username: "alice_vexora",
    })
  );
});

test("Authenticated user: Bob cannot modify Alice's profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Hacked",
      username: "hacked",
    })
  );
});

test("Authenticated user: Alice cannot create post with missing content", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("p1").set({
      userId: ALICE_UID,
    })
  );
});

test("Authenticated user: Alice can create her own post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("posts").doc("p1").set({
      userId: ALICE_UID,
      content: "Hello VEXORA world!",
    })
  );
});

test("Authenticated user: Alice can save valid security settings and Bob cannot read them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("security").doc("config").set({
      userId: ALICE_UID,
      twoFactorEnabled: true,
      privateAccount: false,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("security").doc("config").get()
  );
});

test("Authenticated user: Invalid security settings payload is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("security").doc("config").set({
      userId: ALICE_UID,
      twoFactorEnabled: "yes",
    })
  );
});

test("Authenticated user: Alice can reserve unique username and Bob cannot hijack it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_vexora").set({
      userId: ALICE_UID,
      username: "alice_vexora",
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("usernames").doc("alice_vexora").set({
      userId: BOB_UID,
      username: "alice_vexora",
    })
  );
});

test("Unauthenticated user: cannot read or claim usernames", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("usernames").doc("alice_vexora").get());
  await assertFails(
    unauthDb.collection("usernames").doc("anon_name").set({
      userId: "anon",
      username: "anon_name",
    })
  );
});
