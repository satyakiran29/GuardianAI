// GuardianAI High-Performance Dashboard & Live Telemetry Engine
let liveMap = null;
let markersLayer = null;
let isMapMounted = false;

document.addEventListener('DOMContentLoaded', () => {
  initLazyLiveMap();
  initAutoRefresh();
  initOtpSimulator();
  initInstantTableFilter();
});

// Lazy-Load and Mount Leaflet Map using IntersectionObserver
function initLazyLiveMap() {
  const mapElement = document.getElementById('live-map');
  if (!mapElement) return;

  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (entry.isIntersecting && !isMapMounted) {
          isMapMounted = true;
          // Use requestIdleCallback or small delay to yield to main thread for 60FPS UI
          if ('requestIdleCallback' in window) {
            requestIdleCallback(() => mountLiveMap());
          } else {
            setTimeout(mountLiveMap, 60);
          }
          obs.disconnect();
        }
      });
    }, { rootMargin: '150px 0px' });
    observer.observe(mapElement);
  } else {
    // Fallback for older browsers
    setTimeout(mountLiveMap, 100);
  }
}

// Manual or Programmatic Recenter
function initLiveMap() {
  if (!isMapMounted) {
    mountLiveMap();
  } else if (liveMap) {
    renderMapMarkers();
  }
}

// Mount and configure Leaflet map with tile throttling
function mountLiveMap() {
  const mapElement = document.getElementById('live-map');
  if (!mapElement || typeof L === 'undefined') return;

  if (liveMap) {
    liveMap.remove();
    liveMap = null;
  }

  // Center on default city coordinates (Hyderabad / India)
  liveMap = L.map('live-map', {
    zoomControl: true,
    attributionControl: true,
    preferCanvas: true // Use HTML5 Canvas for high-performance marker rendering
  }).setView([17.4065, 78.4772], 12);

  // CartoDB Dark Matter with lazy tile loading & throttling
  const cartoDark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    subdomains: 'abcd',
    updateWhenIdle: true,     // Only loads new tiles when user stops panning/zooming
    updateWhenZooming: false,  // Saves network requests during zoom transitions
    keepBuffer: 1,             // Reduces in-memory tile cache to keep memory lightweight
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
  });

  // CartoDB Voyager
  const cartoVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    subdomains: 'abcd',
    updateWhenIdle: true,
    updateWhenZooming: false,
    keepBuffer: 1,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
  });

  // OpenStreetMap Standard Street Layer
  const osmStandard = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    updateWhenIdle: true,
    updateWhenZooming: false,
    keepBuffer: 1,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  });

  // Add default dark layer
  cartoDark.addTo(liveMap);

  // Add layer controls for switching styles
  const baseMaps = {
    "🌌 Dark Radar (Default)": cartoDark,
    "🏙️ Voyager Street Map": cartoVoyager,
    "🗺️ OpenStreetMap": osmStandard
  };
  L.control.layers(baseMaps, null, { position: 'topright' }).addTo(liveMap);

  markersLayer = L.layerGroup().addTo(liveMap);

  renderMapMarkers();

  // Smoothly fade out skeleton loader placeholder
  const skeletonEl = document.getElementById('live-map-skeleton');
  if (skeletonEl) {
    skeletonEl.classList.add('fade-out');
  }
}

function renderMapMarkers() {
  if (!markersLayer || typeof MAP_MARKERS === 'undefined') return;

  markersLayer.clearLayers();
  const bounds = [];

  MAP_MARKERS.forEach(item => {
    if (!item.lat || !item.lng) return;

    bounds.push([item.lat, item.lng]);

    if (item.type === 'alert') {
      // Red Radar Pulse Marker for Active SOS
      const pulseHtml = `
        <div class="radar-pulse-marker">
          <div class="wave"></div>
          <div class="core"></div>
        </div>
      `;

      const pulseIcon = L.divIcon({
        className: 'custom-radar-icon',
        html: pulseHtml,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });

      const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${item.lat},${item.lng}`;

      const popupContent = `
        <div style="color: #0f172a; font-family: sans-serif; min-width: 230px;">
          <h4 style="margin: 0 0 6px; color: #ef4444; font-size: 14px; font-weight: bold;">🚨 SOS #${item.id} - ${item.status.toUpperCase()}</h4>
          <p style="margin: 0 0 4px; font-size: 12px;"><strong>Victim:</strong> ${item.user_name} (<code>${item.user_phone}</code>)</p>
          <p style="margin: 0 0 4px; font-size: 12px;"><strong>Trigger:</strong> ${item.trigger_source}</p>
          <p style="margin: 0 0 4px; font-size: 12px;"><strong>Battery:</strong> ${item.battery}%</p>
          <p style="margin: 0 0 8px; font-size: 11px; color: #475569;">📍 <a href="${googleMapsUrl}" target="_blank" style="color: #2563eb; font-weight: 600; text-decoration: none;">${item.address} ↗</a></p>
          <div style="display: flex; gap: 6px; margin-top: 8px;">
            <a href="/alerts/" style="background: #ef4444; color: #fff; padding: 5px 10px; text-decoration: none; border-radius: 6px; font-size: 11px; font-weight: bold;">⚡ Dispatch Unit</a>
            <a href="${googleMapsUrl}" target="_blank" style="background: #2563eb; color: #fff; padding: 5px 10px; text-decoration: none; border-radius: 6px; font-size: 11px; font-weight: bold;">🗺️ Google Maps ↗</a>
          </div>
        </div>
      `;

      const marker = L.marker([item.lat, item.lng], { icon: pulseIcon }).bindPopup(popupContent);
      markersLayer.addLayer(marker);

      // Add emergency zone pulsing radius
      L.circle([item.lat, item.lng], {
        color: '#ef4444',
        fillColor: '#ef4444',
        fillOpacity: 0.15,
        radius: 450
      }).addTo(markersLayer);

    } else if (item.type === 'guardian') {
      // Emerald Shield Marker for Guardian Responders
      const guardHtml = `
        <div style="background: #10b981; border: 2px solid #fff; border-radius: 50%; width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 13px; box-shadow: 0 0 10px #10b981;">
          🛡️
        </div>
      `;

      const guardIcon = L.divIcon({
        className: 'custom-guard-icon',
        html: guardHtml,
        iconSize: [26, 26],
        iconAnchor: [13, 13]
      });

      const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${item.lat},${item.lng}`;

      const guardPopup = `
        <div style="color: #0f172a; font-family: sans-serif; min-width: 190px;">
          <h4 style="margin: 0 0 4px; color: #10b981; font-size: 13px; font-weight: bold;">🛡️ Guardian Unit</h4>
          <p style="margin: 0 0 2px; font-size: 12px;"><strong>${item.name}</strong></p>
          <p style="margin: 0 0 4px; font-size: 11px; color: #475569;">📞 ${item.phone}</p>
          <p style="margin: 0 0 6px; font-size: 11px; color: #64748b;">📍 ${item.address || 'Patrol Route'}</p>
          <a href="${googleMapsUrl}" target="_blank" style="background: #059669; color: #fff; padding: 4px 8px; text-decoration: none; border-radius: 4px; font-size: 10px; font-weight: bold; display: inline-block;">🗺️ Google Maps ↗</a>
        </div>
      `;

      const marker = L.marker([item.lat, item.lng], { icon: guardIcon }).bindPopup(guardPopup);
      markersLayer.addLayer(marker);
    }
  });

  if (bounds.length > 0) {
    liveMap.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
  }
}

// Live Polling for Telemetry & Alert Feed with Tab-Visibility Power Savings
function initAutoRefresh() {
  let isTabActive = true;
  document.addEventListener('visibilitychange', () => {
    isTabActive = !document.hidden;
    if (isTabActive) {
      pollDashboardStats();
    }
  });

  async function pollDashboardStats() {
    if (!isTabActive) return;
    try {
      const response = await fetch('/api/dashboard/stats/');
      if (response.ok) {
        const data = await response.json();
        
        // Update header counters if elements exist
        const sosCountEl = document.getElementById('stat-active-sos');
        if (sosCountEl) sosCountEl.innerText = data.active_alerts;
        
        const totalUsersEl = document.getElementById('stat-total-users');
        if (totalUsersEl) totalUsersEl.innerText = data.total_users;
      }
    } catch (e) {
      console.debug('Polling tick notice:', e);
    }
  }

  setInterval(pollDashboardStats, 10000);
}

// Instant 0ms Client-Side Live Filter for Table Rows
function initInstantTableFilter() {
  const searchInput = document.getElementById('instant-search-input');
  if (!searchInput) return;

  const tableBody = document.querySelector('.cyber-table tbody');
  if (!tableBody) return;

  let debounceTimer = null;
  searchInput.addEventListener('input', (e) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const term = e.target.value.toLowerCase().trim();
      const rows = tableBody.querySelectorAll('tr');
      let matchCount = 0;

      rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (!term || text.includes(term)) {
          row.style.display = '';
          matchCount++;
        } else {
          row.style.display = 'none';
        }
      });

      const countBadge = document.getElementById('filter-match-count');
      if (countBadge) {
        countBadge.innerText = term ? `${matchCount} found` : '';
      }
    }, 120);
  });
}

// OTP Simulator helper
function initOtpSimulator() {
  const btnSendTestOtp = document.getElementById('btn-send-test-otp');
  const btnVerifyTestOtp = document.getElementById('btn-verify-test-otp');

  if (btnSendTestOtp) {
    btnSendTestOtp.addEventListener('click', async () => {
      const targetInput = document.getElementById('test-otp-target');
      const target = targetInput ? targetInput.value.trim() : '';
      const outputDiv = document.getElementById('test-otp-result');

      if (!target || !target.includes('@')) {
        alert('Please enter a valid email address for OTP delivery');
        return;
      }

      try {
        btnSendTestOtp.disabled = true;
        btnSendTestOtp.innerText = '⚡ Dispatching to Email...';

        const res = await fetch('/api/auth/send-otp/', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ target: target, email: target, purpose: 'dashboard_test' })
        });

        const json = await res.json();
        btnSendTestOtp.disabled = false;
        btnSendTestOtp.innerText = '⚡ Dispatch 6-Digit Email OTP Now';

        if (res.ok && json.status === 'success') {
          // Auto-fill verification tester for testing convenience
          const verifyTarget = document.getElementById('verify-otp-target');
          const verifyCode = document.getElementById('verify-otp-code');
          if (verifyTarget) verifyTarget.value = json.target || target;
          if (verifyCode) verifyCode.value = json.otp || '';

          if (outputDiv) {
            outputDiv.innerHTML = `
              <div style="background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; padding: 14px; border-radius: 10px; color: #34d399;">
                <strong>✅ Email OTP Dispatched Successfully!</strong><br>
                <div style="font-size: 0.85rem; color: #cbd5e1; margin-top: 4px;">Recipient: <code>${json.target}</code></div>
                <div style="margin: 6px 0;">Generated Code: <strong style="font-size: 1.3rem; color: #fff; letter-spacing: 3px; font-family: monospace;">${json.otp}</strong></div>
                <div style="font-size: 0.78rem; color: #94a3b8;">
                  Valid for 10 Minutes (600s)${json.email_sent ? ` • 📧 Delivered via Gmail SMTP` : ''}
                </div>
              </div>
            `;
          }
        } else {
          if (outputDiv) {
            outputDiv.innerHTML = `
              <div style="background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; padding: 14px; border-radius: 10px; color: #f87171;">
                <strong>❌ Dispatch Failed</strong><br>
                <span style="font-size: 0.85rem;">${json.message || 'Unable to generate OTP'}</span>
              </div>
            `;
          }
        }
      } catch (e) {
        btnSendTestOtp.disabled = false;
        btnSendTestOtp.innerText = '⚡ Dispatch 6-Digit Email OTP Now';
        alert('Error sending OTP: ' + e);
      }
    });
  }

  if (btnVerifyTestOtp) {
    btnVerifyTestOtp.addEventListener('click', async () => {
      const targetInput = document.getElementById('verify-otp-target');
      const codeInput = document.getElementById('verify-otp-code');
      const outputDiv = document.getElementById('verify-otp-result');

      const target = targetInput ? targetInput.value.trim() : '';
      const otpCode = codeInput ? codeInput.value.trim() : '';

      if (!target || !otpCode) {
        alert('Please enter both email address and 6-digit passcode');
        return;
      }

      try {
        btnVerifyTestOtp.disabled = true;
        btnVerifyTestOtp.innerText = 'Verifying Passcode...';

        const res = await fetch('/api/auth/verify-otp/', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ target: target, otp_code: otpCode })
        });

        const json = await res.json();
        btnVerifyTestOtp.disabled = false;
        btnVerifyTestOtp.innerText = '✓ Verify Passcode Now';

        if (res.ok && json.status === 'success') {
          if (outputDiv) {
            outputDiv.innerHTML = `
              <div style="background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; padding: 14px; border-radius: 10px; color: #34d399;">
                <strong>✓ VERIFICATION SUCCESSFUL!</strong><br>
                <div style="font-size: 0.85rem; color: #cbd5e1; margin-top: 4px;">Target: <code>${json.target}</code></div>
                <div style="font-size: 0.82rem; color: #34d399; margin-top: 2px;">${json.message}</div>
              </div>
            `;
          }
        } else {
          if (outputDiv) {
            outputDiv.innerHTML = `
              <div style="background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; padding: 14px; border-radius: 10px; color: #f87171;">
                <strong>❌ Verification Rejected</strong><br>
                <span style="font-size: 0.85rem;">${json.message || 'Invalid or expired OTP code'}</span>
              </div>
            `;
          }
        }
      } catch (e) {
        btnVerifyTestOtp.disabled = false;
        btnVerifyTestOtp.innerText = '✓ Verify Passcode Now';
        alert('Error verifying OTP: ' + e);
      }
    });
  }
}


// Modal Toggle Helpers
function openModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.add('active');
}

function closeModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.remove('active');
}
