
const CACHE_NAME = 'quran-audio-v2';
const AUDIO_FILES = [
  '/001-al-fatihah.mp3',
  '/002-al-baqarah.mp3',
  // Add other frequently played surahs
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(AUDIO_FILES))
  );
});

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then(response => response || fetch(event.request))
  );
});