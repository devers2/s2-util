// Static server for the browser tests and manual demo. Serves s2.validator.js at the same path as the jar
// (/s2-util/js/s2.validator.js) and the demo pages at /. | 브라우저 시험·수동 데모용 정적 서버. s2.validator.js 를 jar 와
// 같은 경로(/s2-util/js/s2.validator.js)로, 데모 페이지를 / 로 제공
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT || 4173);
const DEMO_DIR = path.join(__dirname, 'demo');
const SITE_DIR = path.join(__dirname, 'site');
const JAR_RESOURCES = path.join(__dirname, '..', 'src', 'main', 'resources', 'META-INF', 'resources');
const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8'
};

http
  .createServer((req, res) => {
    let urlPath = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
    let base = urlPath.startsWith('/s2-util/') ? JAR_RESOURCES : DEMO_DIR;
    // 공개 데모(GitHub Pages 와 같은 상대 경로): /site/ → site/, /site/s2.validator.js → jar 의 스크립트
    if (urlPath === '/site/s2.validator.js') {
      base = JAR_RESOURCES;
      urlPath = '/s2-util/js/s2.validator.js';
    } else if (urlPath === '/site' || urlPath.startsWith('/site/')) {
      base = SITE_DIR;
      urlPath = urlPath.slice('/site'.length) || '/';
    }
    const file = path.normalize(path.join(base, urlPath === '/' ? 'index.html' : urlPath));
    if (!file.startsWith(base) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    fs.createReadStream(file).pipe(res);
  })
  .listen(PORT, () => console.log(`s2.validator demo: http://localhost:${PORT}/ (public site: /site/)`));
