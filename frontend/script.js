const apiBaseInput = document.getElementById("apiBaseUrl");
const bookingApiBaseInput = document.getElementById("bookingApiBaseUrl");
const notificationApiBaseInput = document.getElementById("notificationApiBaseUrl");
const loadCarsButton = document.getElementById("loadCarsButton");
const vipCarSelect = document.getElementById("vipCarSelect");
const bookVipButton = document.getElementById("bookVipButton");
const stationsStatus = document.getElementById("stationsStatus");
const vipResult = document.getElementById("vipResult");
const carsContainer = document.getElementById("carsContainer");
const errorBox = document.getElementById("error");
let stationStatusEventSource = null;

function clearError() {
  errorBox.textContent = "";
}

function showError(message) {
  errorBox.textContent = message;
}

function apiBaseUrl() {
  return apiBaseInput.value.trim().replace(/\/+$/, "");
}

function bookingApiBaseUrl() {
  return bookingApiBaseInput.value.trim().replace(/\/+$/, "");
}

function notificationApiBaseUrl() {
  return notificationApiBaseInput.value.trim().replace(/\/+$/, "");
}

function carStatusDotClass(status) {
  if (status === "CHARGING") return "green";
  if (status === "DRIVING") return "yellow";
  if (status === "STOPPED") return "red";
  return "gray";
}

function colorFromVin(vin) {
  let hash = 0;
  for (let i = 0; i < vin.length; i += 1) {
    hash = (hash * 31 + vin.charCodeAt(i)) & 0xffffffff;
  }
  const palette = [
    ["#1d4ed8", "#60a5fa"],
    ["#7c3aed", "#a78bfa"],
    ["#0f766e", "#2dd4bf"],
    ["#b45309", "#f59e0b"],
    ["#be123c", "#fb7185"]
  ];
  return palette[Math.abs(hash) % palette.length];
}

function carImageDataUrl(car) {
  const [bgStart, bgEnd] = colorFromVin(car.vin);
  const badge = car.vipEligible ? "VIP" : "CORE";
  const model = String(car.name ?? "VOLVO").toUpperCase();
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="480" height="210" viewBox="0 0 480 210">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${bgStart}" />
      <stop offset="100%" stop-color="${bgEnd}" />
    </linearGradient>
  </defs>
  <rect width="480" height="210" rx="20" fill="url(#bg)" />
  <rect x="16" y="16" width="84" height="30" rx="10" fill="rgba(255,255,255,0.2)" />
  <text x="58" y="36" fill="white" font-size="16" font-family="Arial, sans-serif" text-anchor="middle">${badge}</text>
  <g fill="rgba(255,255,255,0.88)">
    <path d="M96 130c8-30 26-46 52-46h126c30 0 44 10 66 36l24 10c7 3 13 12 13 20v13h-26a26 26 0 0 1-52 0H182a26 26 0 0 1-52 0H96z"/>
    <circle cx="156" cy="165" r="21" fill="#111827"/>
    <circle cx="156" cy="165" r="10" fill="#9ca3af"/>
    <circle cx="325" cy="165" r="21" fill="#111827"/>
    <circle cx="325" cy="165" r="10" fill="#9ca3af"/>
  </g>
  <text x="24" y="194" fill="white" font-size="24" font-family="Arial, sans-serif" font-weight="700">${model}</text>
</svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function renderStationsStatus(stations) {
  stationsStatus.innerHTML = "";
  const sorted = [...stations].sort((a, b) => {
    if (a.stationType === b.stationType) {
      return a.stationName.localeCompare(b.stationName);
    }
    if (a.stationType === "VIP") return 1;
    if (b.stationType === "VIP") return -1;
    return a.stationType.localeCompare(b.stationType);
  });

  for (const station of sorted) {
    const isFree = station.status === "FREE";
    const dotClass = isFree ? "green" : "red";
    const card = document.createElement("article");
    card.className = "station-card";
    card.innerHTML = `
      <h3 class="station-name">${station.stationName}</h3>
      <div><strong>Type:</strong> ${station.stationType}</div>
      <div><strong>Status:</strong> <span class="status-dot ${dotClass}"></span>${station.status}</div>
      <div><strong>Assigned VIN:</strong> ${station.assignedVin ?? "None"}</div>
    `;
    stationsStatus.appendChild(card);
  }
}

async function loadInitialStationsStatus() {
  try {
    const response = await fetch(`${notificationApiBaseUrl()}/api/stations-status`);
    if (!response.ok) {
      throw new Error(`Failed to load station status (${response.status})`);
    }
    renderStationsStatus(await response.json());
  } catch (error) {
    showError(error.message);
  }
}

function connectStationStream() {
  if (stationStatusEventSource) {
    stationStatusEventSource.close();
  }
  stationStatusEventSource = new EventSource(`${notificationApiBaseUrl()}/api/stations-status/stream`);
  stationStatusEventSource.addEventListener("station-status", (event) => {
    try {
      renderStationsStatus(JSON.parse(event.data));
    } catch (error) {
      showError(`Invalid SSE payload: ${event.data}`);
    }
  });
  stationStatusEventSource.onerror = () => {
    showError("SSE connection to notification-service interrupted.");
  };
}

async function loadCars() {
  clearError();
  carsContainer.innerHTML = "Loading cars...";
  try {
    const response = await fetch(`${apiBaseUrl()}/api/cars`);
    if (!response.ok) {
      throw new Error(`Failed to load cars (${response.status})`);
    }
    const cars = await response.json();
    renderCars(cars);
    renderVipCars(cars.filter((car) => car.vipEligible));
  } catch (error) {
    carsContainer.innerHTML = "";
    showError(error.message);
  }
}

function renderCars(cars) {
  carsContainer.innerHTML = "";
  for (const car of cars) {
    const card = document.createElement("article");
    card.className = "car-card";
    card.innerHTML = `
      <div class="car-image-wrap">
        <img class="car-image" alt="${car.name} illustration" src="${carImageDataUrl(car)}" />
      </div>
      <h3 class="car-title">${car.name}</h3>
      <p class="car-vin">VIN: ${car.vin}</p>
      <p class="car-vin">VIP eligible: ${car.vipEligible ? "Yes" : "No"}</p>
      <button type="button">Get status</button>
      <div class="status" id="status-${car.vin}">
        <strong>Status:</strong> <span class="status-dot gray"></span>Unknown<br>
        <strong>Battery:</strong> -<br>
        <strong>Station:</strong> -
      </div>
    `;

    const button = card.querySelector("button");
    const statusBox = card.querySelector(".status");
    button.addEventListener("click", () => loadStatus(car.vin, statusBox));

    carsContainer.appendChild(card);
  }
}

function renderVipCars(vipCars) {
  vipCarSelect.innerHTML = "";
  for (const car of vipCars) {
    const option = document.createElement("option");
    option.value = car.vin;
    option.textContent = `${car.name} (${car.vin})`;
    vipCarSelect.appendChild(option);
  }
  if (vipCars.length === 0) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "No VIP cars found";
    vipCarSelect.appendChild(option);
  }
}

async function loadStatus(vin, statusBox) {
  clearError();
  statusBox.textContent = "Loading...";
  try {
    const response = await fetch(`${apiBaseUrl()}/api/cars/${vin}`);
    if (!response.ok) {
      throw new Error(`Failed to load status for ${vin} (${response.status})`);
    }
    const details = await response.json();
    const dotClass = carStatusDotClass(details.status);
    statusBox.innerHTML = `
      <strong>Status:</strong> <span class="status-dot ${dotClass}"></span>${details.status}<br>
      <strong>Battery:</strong> ${details.batteryPercentage}%<br>
      <strong>Station:</strong> ${details.assignedChargingStationId ?? "None"}
    `;
  } catch (error) {
    statusBox.textContent = "Could not load status.";
    showError(error.message);
  }
}

async function bookVipStation() {
  clearError();
  const vin = vipCarSelect.value;
  if (!vin) {
    showError("No VIP car selected.");
    return;
  }

  vipResult.textContent = "Booking VIP station...";
  try {
    const bookResponse = await fetch(`${bookingApiBaseUrl()}/api/vip-stations/book?vin=${encodeURIComponent(vin)}`, {
      method: "POST"
    });
    const bookPayload = await bookResponse.json();
    if (!bookResponse.ok || !bookPayload.success) {
      throw new Error(bookPayload.message ?? `VIP booking failed (${bookResponse.status})`);
    }
    vipResult.textContent = `Booked VIP station ${bookPayload.chargingStationId} for car ${vin}. Simulator will start charging via Kafka.`;
  } catch (error) {
    vipResult.textContent = "VIP booking failed.";
    showError(error.message);
  }
}

loadCarsButton.addEventListener("click", loadCars);
bookVipButton.addEventListener("click", bookVipStation);
loadCars();
loadInitialStationsStatus();
connectStationStream();
