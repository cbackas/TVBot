/* eslint-disable no-template-curly-in-string */
/**
 * @typedef {import('semantic-release').Options} Options
 */

/** @type {Options} */
module.exports = {
  branches: ["main"],
  tagFormat: "${version}",
  repositoryUrl: "https://github.com/cbackas/TVBot",
  plugins: [
    "@semantic-release/commit-analyzer",
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
    [
      "@codedependant/semantic-release-docker",
      {
        dockerTags: ["{{version}}", "latest"],
        dockerFile: "Dockerfile",
        dockerPlatform: ["linux/amd64", "linux/arm64"],
        dockerRegistry: "ghcr.io",
        dockerProject: "cbackas",
        dockerImage: "tvbot",
      },
    ],
  ],
  preset: "eslint",
};
