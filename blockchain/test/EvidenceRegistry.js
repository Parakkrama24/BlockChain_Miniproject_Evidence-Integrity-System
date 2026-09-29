const { expect } = require("chai");
const { ethers } = require("hardhat");
const { anyValue } = require("@nomicfoundation/hardhat-chai-matchers/withArgs");



describe("EvidenceRegistry", function () {
  let evidenceRegistry;
  let owner;

  beforeEach(async function () {
    [owner] = await ethers.getSigners();
    const EvidenceRegistry = await ethers.getContractFactory("EvidenceRegistry");
    evidenceRegistry = await EvidenceRegistry.deploy();
  });

  // tests go here
  it("should register evidence and store correct data", async function () {
    const caseId = "CASE-001";
    const fileName = "evidence.pdf";
    const hash = ethers.sha256(ethers.toUtf8Bytes("dummy file content"));

    const tx = await evidenceRegistry.registerEvidence(caseId, fileName, hash);
    await tx.wait();

    const record = await evidenceRegistry.records(0);

    expect(record.caseId).to.equal(caseId);
    expect(record.fileName).to.equal(fileName);
    expect(record.sha256Hash).to.equal(hash);
    expect(record.uploader).to.equal(owner.address);
  });

  it("should emit EvidenceRegistered event with correct args", async function () {
    const caseId = "CASE-002";
    const fileName = "photo.jpg";
    const hash = ethers.sha256(ethers.toUtf8Bytes("another file"));

    await expect(evidenceRegistry.registerEvidence(caseId, fileName, hash))
      .to.emit(evidenceRegistry, "EvidenceRegistered")
      .withArgs(0, caseId, hash, owner.address, anyValue);
  });

  it("should assign incrementing IDs to multiple registrations", async function () {
    const hash1 = ethers.sha256(ethers.toUtf8Bytes("file one"));
    const hash2 = ethers.sha256(ethers.toUtf8Bytes("file two"));

    const tx1 = await evidenceRegistry.registerEvidence("CASE-001", "a.pdf", hash1);
    await tx1.wait();

    const tx2 = await evidenceRegistry.registerEvidence("CASE-001", "b.pdf", hash2);
    await tx2.wait();

    const record0 = await evidenceRegistry.records(0);
    const record1 = await evidenceRegistry.records(1);

    expect(record0.fileName).to.equal("a.pdf");
    expect(record1.fileName).to.equal("b.pdf");
    expect(await evidenceRegistry.nextId()).to.equal(2);
  });

  it("should return an evidence record with getEvidence", async function () {
    const hash = ethers.sha256(ethers.toUtf8Bytes("stored evidence"));
    await evidenceRegistry.registerEvidence("CASE-003", "report.pdf", hash);

    const record = await evidenceRegistry.getEvidence(0);

    expect(record.caseId).to.equal("CASE-003");
    expect(record.fileName).to.equal("report.pdf");
    expect(record.sha256Hash).to.equal(hash);
    expect(record.uploader).to.equal(owner.address);
  });

  it("should verify the exact original hash", async function () {
    const originalBytes = ethers.toUtf8Bytes("evidence bytes");
    const originalHash = ethers.sha256(originalBytes);
    await evidenceRegistry.registerEvidence("CASE-004", "evidence.bin", originalHash);

    expect(await evidenceRegistry.verifyHash(0, originalHash)).to.equal(true);
  });

  it("should detect a one-byte modification using different SHA-256 hashes", async function () {
    const originalBytes = ethers.toUtf8Bytes("evidence bytes");
    const modifiedBytes = ethers.getBytes(ethers.hexlify(originalBytes));
    modifiedBytes[modifiedBytes.length - 1] ^= 1;
    const originalHash = ethers.sha256(originalBytes);
    const modifiedHash = ethers.sha256(modifiedBytes);
    await evidenceRegistry.registerEvidence("CASE-005", "evidence.bin", originalHash);

    expect(modifiedHash).to.not.equal(originalHash);
    expect(await evidenceRegistry.verifyHash(0, modifiedHash)).to.equal(false);
  });

  it("should return a zeroed record for an unknown evidence ID", async function () {
    const record = await evidenceRegistry.getEvidence(999);

    expect(record.caseId).to.equal("");
    expect(record.fileName).to.equal("");
    expect(record.sha256Hash).to.equal(ethers.ZeroHash);
    expect(record.uploader).to.equal(ethers.ZeroAddress);
    expect(record.timestamp).to.equal(0);
  });
});
