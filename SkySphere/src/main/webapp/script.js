/* ================================================================
   SkySphere — script.js  (UPGRADED: Live Weather API version)
   New fields shown: feelsLike, minTemp, maxTemp, pressure, cityName
================================================================ */

// ── State Meta (images + emoji only — temps now come from API) ───
const STATE_META = {
  "Tamil Nadu":     { emoji: "☀️",  image: "images/tamil_nadu.jpg"     },
  "Kerala":         { emoji: "🌧️", image: "images/kerala.jpg"          },
  "Karnataka":      { emoji: "⛅",  image: "images/karnataka.jpg"       },
  "Andhra Pradesh": { emoji: "🌤️", image: "images/andhra_pradesh.jpg"  },
  "Telangana":      { emoji: "🌞",  image: "images/telangana.jpg"       },
  "Maharashtra":    { emoji: "🌥️", image: "images/maharashtra.jpg"     },
  "Gujarat":        { emoji: "☀️",  image: "images/gujarat.jpg"         },
  "Rajasthan":      { emoji: "🔥",  image: "images/rajasthan.jpg"       },
  "Punjab":         { emoji: "💨",  image: "images/punjab.jpg"          },
  "West Bengal":    { emoji: "⛈️", image: "images/west_bengal.jpg"     }
};

// City shown in card subtitle (matches WeatherServlet STATE_TO_CITY map)
const STATE_TO_CITY = {
  "Tamil Nadu":     "Chennai",
  "Kerala":         "Thiruvananthapuram",
  "Karnataka":      "Bangalore",
  "Andhra Pradesh": "Visakhapatnam",
  "Telangana":      "Hyderabad",
  "Maharashtra":    "Mumbai",
  "Gujarat":        "Ahmedabad",
  "Rajasthan":      "Jaipur",
  "Punjab":         "Chandigarh",
  "West Bengal":    "Kolkata"
};

// ── Map weather condition text → Font Awesome icon + color ───────
function getIconForCondition(condition) {
  const c = condition.toLowerCase();
  if (c.includes("thunder") || c.includes("storm"))  return { icon: "fa-solid fa-cloud-bolt",      css: "icon-thunder" };
  if (c.includes("rain")    || c.includes("drizzle"))return { icon: "fa-solid fa-cloud-rain",      css: "icon-rainy"   };
  if (c.includes("snow")    || c.includes("sleet"))  return { icon: "fa-solid fa-snowflake",        css: "icon-snow"    };
  if (c.includes("fog")     || c.includes("mist")
                             || c.includes("haze"))  return { icon: "fa-solid fa-smog",             css: "icon-cloudy"  };
  if (c.includes("cloud"))                           return { icon: "fa-solid fa-cloud-sun",        css: "icon-cloudy"  };
  if (c.includes("wind"))                            return { icon: "fa-solid fa-wind",             css: "icon-windy"   };
  if (c.includes("smoke")   || c.includes("dust"))  return { icon: "fa-solid fa-smog",             css: "icon-hot"     };
  return { icon: "fa-solid fa-sun", css: "icon-sunny" };   // Default: clear/sunny
}

// ── DOM References ───────────────────────────────────────────────
const stateSelect  = document.getElementById("stateSelect");
const loader       = document.getElementById("loader");
const errorMsg     = document.getElementById("errorMsg");
const errorText    = document.getElementById("errorText");
const weatherCard  = document.getElementById("weatherCard");
const historyWrap  = document.getElementById("historyWrap");
const historyBody  = document.getElementById("historyBody");
const historyEmpty = document.getElementById("historyEmpty");
const darkToggle   = document.getElementById("darkToggle");
const toggleIcon   = document.getElementById("toggleIcon");

// ── Dark Mode ────────────────────────────────────────────────────
function initDarkMode() {
  const saved = localStorage.getItem("skysphere-theme");
  if (saved === "dark") applyDark(); else applyLight();
}
function applyDark()  {
  document.body.classList.add("dark-mode"); document.body.classList.remove("light-mode");
  toggleIcon.className = "fa-solid fa-sun";
  localStorage.setItem("skysphere-theme", "dark");
}
function applyLight() {
  document.body.classList.add("light-mode"); document.body.classList.remove("dark-mode");
  toggleIcon.className = "fa-solid fa-moon";
  localStorage.setItem("skysphere-theme", "light");
}
darkToggle.addEventListener("click", () => {
  document.body.classList.contains("dark-mode") ? applyLight() : applyDark();
});

// ── Show/Hide helpers ─────────────────────────────────────────────
function showLoader()   { loader.style.display = "flex";  }
function hideLoader()   { loader.style.display = "none";  }
function showError(msg) { errorText.textContent = msg; errorMsg.style.display = "flex"; }
function hideError()    { errorMsg.style.display = "none"; }
function hideCard()     { weatherCard.style.display = "none"; }

// ── Main Search — calls live WeatherServlet ───────────────────────
function searchWeather() {
  const state = stateSelect.value.trim();
  if (!state) { showError("Please select a state first."); return; }

  hideError();
  hideCard();
  showLoader();

  // Calls your Java servlet — which calls OpenWeatherMap API
  fetch("WeatherServlet?state=" + encodeURIComponent(state))
    .then(res => {
      if (!res.ok) throw new Error("Server error: " + res.status);
      return res.json();
    })
    .then(data => {
      hideLoader();
      if (data.error) { showError(data.error); return; }
      populateCard(data);
      weatherCard.style.display = "flex";
    })
    .catch(err => {
      hideLoader();
      showError("Cannot reach server. Is Tomcat running? (" + err.message + ")");
    });
}

// ── Populate Weather Card with LIVE data ──────────────────────────
function populateCard(data) {
  const stateName  = data.stateName  || "Unknown";
  const cityName   = data.cityName   || STATE_TO_CITY[stateName] || "";
  const temp       = data.temperature       || "N/A";
  const condition  = data.weatherCondition  || "N/A";
  const humidity   = data.humidity          || "N/A";
  const wind       = data.wind              || "N/A";
  const visibility = data.visibility        || "N/A";
  const feelsLike  = data.feelsLike         || "N/A";
  const minTemp    = data.minTemp           || "N/A";
  const maxTemp    = data.maxTemp           || "N/A";
  const pressure   = data.pressure          || "N/A";
  const searchTime = data.searchTime        || new Date().toLocaleString();

  // ── Basic fields ──────────────────────────────────────────────
  document.getElementById("cardState").textContent     = stateName;
  document.getElementById("cardTemp").textContent      = temp;
  document.getElementById("cardCondition").textContent = condition;
  document.getElementById("searchedAt").textContent    = "Live data · " + searchTime;

  // Show city name below state name (new)
  const cityEl = document.getElementById("cardCity");
  if (cityEl) cityEl.textContent = cityName;

  // ── Metric boxes ──────────────────────────────────────────────
  document.getElementById("cardHumidity").textContent   = humidity;
  document.getElementById("cardWind").textContent       = wind;
  document.getElementById("cardVisibility").textContent = visibility;

  // ── Extra live fields (new) ───────────────────────────────────
  const feelsEl = document.getElementById("cardFeelsLike");
  if (feelsEl) feelsEl.textContent = feelsLike;

  const minEl = document.getElementById("cardMinTemp");
  if (minEl) minEl.textContent = minTemp;

  const maxEl = document.getElementById("cardMaxTemp");
  if (maxEl) maxEl.textContent = maxTemp;

  const pressEl = document.getElementById("cardPressure");
  if (pressEl) pressEl.textContent = pressure;

  // ── State image ───────────────────────────────────────────────
  const meta  = STATE_META[stateName] || {};
  const imgEl = document.getElementById("stateImg");
  imgEl.src   = meta.image || "images/default.jpg";
  imgEl.alt   = stateName;

  // ── Weather icon ──────────────────────────────────────────────
  const iconInfo = getIconForCondition(condition);
  const iconEl   = document.getElementById("weatherIcon");
  iconEl.className = iconInfo.icon + " weather-icon " + iconInfo.css;

  // ── Re-trigger slide-in animation ────────────────────────────
  weatherCard.style.animation = "none";
  weatherCard.offsetHeight;
  weatherCard.style.animation = "slideUp 0.5s cubic-bezier(.22,1,.36,1) both";
}

// ── Share button ──────────────────────────────────────────────────
function shareWeather() {
  const state = document.getElementById("cardState").textContent;
  const temp  = document.getElementById("cardTemp").textContent;
  const cond  = document.getElementById("cardCondition").textContent;
  const text  = `🌤️ ${state}: ${temp}, ${cond} — Live data from SkySphere`;
  if (navigator.share) {
    navigator.share({ title: "SkySphere", text });
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => alert("Copied to clipboard!"));
  } else { alert(text); }
}

// ── Load Search History ───────────────────────────────────────────
function loadHistory() {
  historyWrap.style.display  = "none";
  historyEmpty.style.display = "none";
  historyBody.innerHTML      = "";

  fetch("HistoryServlet")
    .then(res => { if (!res.ok) throw new Error("HTTP " + res.status); return res.json(); })
    .then(rows => {
      if (!rows || rows.length === 0) { historyEmpty.style.display = "block"; return; }
      rows.forEach(row => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${row.id}</td>
          <td>${row.stateName}</td>
          <td>${row.temperature}</td>
          <td>${row.weatherCondition}</td>
          <td>${row.searchTime}</td>`;
        historyBody.appendChild(tr);
      });
      historyWrap.style.display = "block";
    })
    .catch(err => {
      historyEmpty.style.display = "block";
      historyEmpty.querySelector("p").textContent = "Error: " + err.message;
    });
}

function clearHistoryUI() {
  historyWrap.style.display  = "none";
  historyEmpty.style.display = "none";
}

// ── Quick-pick State Cards Grid ───────────────────────────────────
function buildStatesGrid() {
  const grid = document.getElementById("statesGrid");
  if (!grid) return;

  Object.entries(STATE_META).forEach(([name, meta], idx) => {
    const card = document.createElement("div");
    card.className = "state-quick-card";
    card.style.animationDelay = `${idx * 0.05}s`;
    card.innerHTML = `
      <div class="sqc-icon">${meta.emoji}</div>
      <p class="sqc-name">${name}</p>
      <p class="sqc-temp" style="font-size:.75rem;color:var(--text-muted)">${STATE_TO_CITY[name]}</p>`;
    card.addEventListener("click", () => {
      stateSelect.value = name;
      document.getElementById("dashboard").scrollIntoView({ behavior: "smooth" });
      setTimeout(searchWeather, 400);
    });
    grid.appendChild(card);
  });
}

// ── Enter key triggers search ─────────────────────────────────────
stateSelect.addEventListener("keydown", e => { if (e.key === "Enter") searchWeather(); });

// ── Init ──────────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
  initDarkMode();
  buildStatesGrid();
});

// Active navbar link on scroll
const sections = document.querySelectorAll("section, main");
const navLinks = document.querySelectorAll(".nav-link");

window.addEventListener("scroll", () => {
  let current = "";

  sections.forEach(section => {
    const sectionTop = section.offsetTop - 120;
    const sectionHeight = section.offsetHeight;

    if (window.scrollY >= sectionTop &&
        window.scrollY < sectionTop + sectionHeight) {
      current = section.getAttribute("id");
    }
  });

  navLinks.forEach(link => {
    link.classList.remove("active");

    const href = link.getAttribute("href").substring(1);

    if (href === current) {
      link.classList.add("active");
    }
  });
});