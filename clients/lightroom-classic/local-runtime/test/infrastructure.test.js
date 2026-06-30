const assert = require('assert')
const fs = require('fs')
const path = require('path')
const test = require('node:test')

const repoRoot = path.join(__dirname, '..', '..', '..', '..')

test('docker compose includes Milvus vector database instead of Qdrant', () => {
  const compose = fs.readFileSync(path.join(repoRoot, 'docker-compose.yml'), 'utf8')

  assert.match(compose, /milvus:/)
  assert.match(compose, /milvusdb\/milvus:/)
  assert.match(compose, /etcd:/)
  assert.match(compose, /19530:19530/)
  assert.match(compose, /milvus-data:/)
  assert.doesNotMatch(compose, /qdrant/i)
})

test('local compose launcher starts Milvus and injects vector store settings', () => {
  const script = fs.readFileSync(path.join(repoRoot, 'scripts', 'start-local-compose.sh'), 'utf8')

  assert.match(script, /docker compose up -d mysql redis minio etcd milvus/)
  assert.match(script, /TONEPILOT_VECTOR_STORE="milvus"/)
  assert.match(script, /MILVUS_URI="http:\/\/localhost:19530"/)
  assert.match(script, /MySQL、Redis、MinIO、Milvus/)
})


test('tracked project files do not expose personal absolute paths', () => {
  const { execFileSync } = require('child_process')
  const files = execFileSync('git', ['ls-files'], { cwd: repoRoot, encoding: 'utf8' })
    .split('\n')
    .filter(Boolean)
  const userName = 'lvchang' + 'hong'
  const legacyProjectName = 'TonePilot-' + 'scaffold'
  const legacyWorkspaceName = '摄影' + '调色agent'
  const forbidden = [
    new RegExp('/' + 'home/' + userName, 'i'),
    new RegExp('/mnt/c/' + 'Users/' + userName, 'i'),
    new RegExp('C:' + '\\Users\\' + userName, 'i'),
    new RegExp('C:' + '/Users/' + userName, 'i'),
    new RegExp(userName, 'i'),
    new RegExp(legacyProjectName, 'i'),
    new RegExp(legacyWorkspaceName)
  ]
  const offenders = []

  for (const file of files) {
    const fullPath = path.join(repoRoot, file)
    if (!fs.existsSync(fullPath) || fs.statSync(fullPath).isDirectory()) continue
    const content = fs.readFileSync(fullPath, 'utf8')
    if (forbidden.some((pattern) => pattern.test(content))) {
      offenders.push(file)
    }
  }

  assert.deepStrictEqual(offenders, [])
})
