/* eslint-disable indent */
/* eslint-disable no-template-curly-in-string */
/**
 * @typedef {import('semantic-release').Options} Options
 */

/** @type {Options} */
module.exports = {
  branches: ['main'],
  tagFormat: '${version}',
  repositoryUrl: 'https://github.com/cbackas/TVBot',
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'eslint'
      }
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'eslint'
      }
    ],
    [
      '@semantic-release/github',
      {
        failTitle: false,
        failComment: false,
        labels: false,
        releasedLabels: false
      }
    ],
    [
        '@codedependant/semantic-release-docker',
        {
          dockerTags: ['{{version}}', 'latest'],
          dockerFile: 'Dockerfile',
          dockerRegistry: 'ghcr.io',
          dockerProject: 'cbackas',
          dockerImage: 'tvbot'
        }
      ]
  ]
}
