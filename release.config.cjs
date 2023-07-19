/* eslint-disable indent */
/* eslint-disable no-template-curly-in-string */
/**
 * @typedef {import('semantic-release').Options} Options
 */

const releaseRules = [
  { tag: 'Breaking', release: 'major' },
  { tag: 'Fix', release: 'patch' },
  { tag: 'Update', release: 'minor' },
  { tag: 'New', release: 'minor' },
  { tag: 'Build', release: 'patch' },
  { tag: 'Upgrade', release: 'patch' },
  { tag: 'Chore', release: 'patch' }
]

const noteKeywords = releaseRules.map(rule => rule.tag)
/** @type {Options} */

module.exports = {
  branches: ['main'],
  tagFormat: '${version}',
  repositoryUrl: 'https://github.com/cbackas/TVBot',
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'eslint',
        releaseRules
      }
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'eslint',
        parserOpts: {
          noteKeywords
        }
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
