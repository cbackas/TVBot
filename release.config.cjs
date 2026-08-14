/* eslint-disable no-template-curly-in-string */
/**
 * @typedef {import('npm:semantic-release').Options} Options
 */

/** @type {Options} */
module.exports = {
  branches: ["main"],
  tagFormat: "${version}",
  repositoryUrl: "https://github.com/cbackas/TVBot",
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        releaseRules: [
          { breaking: true, release: "major" },
          { revert: true, release: "patch" },
          { type: "feat", release: "minor" },
          { release: "patch" },
        ],
      },
    ],
    "@semantic-release/release-notes-generator",
    [
      "@semantic-release/github",
      {
        failTitle: false,
        failComment: false,
        labels: false,
        releasedLabels: false,
      },
    ],
  ],
}
