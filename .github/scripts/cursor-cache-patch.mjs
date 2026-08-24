/**
 * One-shot Cursor cloud agent for a failing cache-watchdog run.
 *
 * Official invoke (same as this script):
 *   export CURSOR_API_KEY  # never echo
 *   node .github/scripts/cursor-cache-patch.mjs
 *
 * Uses Agent.prompt() + cloud.autoCreatePR from @cursor/sdk.
 * Requires the Qodat/qodat GitHub integration on the Cursor account that
 * minted CURSOR_API_KEY (Dashboard → Integrations).
 */
import { Agent, CursorAgentError } from "@cursor/sdk";

const apiKey = process.env.CURSOR_API_KEY;
if (!apiKey) {
  console.error("CURSOR_API_KEY is unset; not starting an agent");
  process.exit(0);
}

const repoUrl = process.env.QODAT_REPO_URL || "https://github.com/Qodat/qodat";
const startingRef = process.env.QODAT_STARTING_REF || "master";
const archiveName = process.env.QODAT_CACHE_ARCHIVE || "unknown-archive";
const issueUrl = process.env.QODAT_ISSUE_URL || "";
const smokeSummary = (process.env.QODAT_SMOKE_SUMMARY || "").slice(0, 8000);

const prompt = [
  "The scheduled Qodat OSRS-cache decoder watchdog failed on the latest published cache.",
  `Cache archive: ${archiveName}`,
  issueUrl ? `GitHub issue: ${issueUrl}` : "",
  "",
  "Smoke summary:",
  smokeSummary || "(no summary attached)",
  "",
  "Task:",
  "- Patch only qodat decoder/loader code in this repo so the new cache revision decodes.",
  "- Stay inside cache/model/interface/animation loaders (for example src/main/kotlin/stan/qodat/cache/, qodat-api model loaders).",
  "- No drive-by refactors, formatting sweeps, dependency bumps, or unrelated files.",
  "- Do not commit the OSRS cache or any downloaded archive.",
  "- Add or extend focused unit tests with synthetic payloads when that is enough; do not vendor cache binaries.",
  "- Open a pull request that explains the revision/opcode change.",
].filter((line) => line !== undefined).join("\n");

try {
  const result = await Agent.prompt(prompt, {
    apiKey,
    model: { id: "composer-2.5" },
    cloud: {
      repos: [{ url: repoUrl, startingRef }],
      autoCreatePR: true,
      skipReviewerRequest: true,
    },
  });
  console.log("run.id=" + result.id);
  console.log("run.status=" + result.status);
  if (result.git?.branches?.[0]?.prUrl) {
    console.log("pr=" + result.git.branches[0].prUrl);
  } else if (result.git?.branches?.[0]?.branch) {
    console.log("branch=" + result.git.branches[0].branch);
  }
  if (result.status === "error") {
    console.error("run failed: " + result.id);
    process.exit(2);
  }
} catch (err) {
  if (err instanceof CursorAgentError) {
    console.error("startup failed: " + err.message + ", retryable=" + err.isRetryable);
    process.exit(1);
  }
  throw err;
}
