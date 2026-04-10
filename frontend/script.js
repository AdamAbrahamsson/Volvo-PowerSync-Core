const bookVipButton = document.getElementById("bookVipButton");
const vipSelectionHint = document.getElementById("vipSelectionHint");
const stationsStatus = document.getElementById("stationsStatus");
const vipResult = document.getElementById("vipResult");
const carsContainer = document.getElementById("carsContainer");
const errorBox = document.getElementById("error");
const toastContainer = document.getElementById("toastContainer");
let stationStatusEventSource = null;
let carStatusEventSource = null;
let stationNamesById = new Map();
let statusBoxByVin = new Map();
let vipBookingInFlight = false;
// Same host in prod (Ingress + Cloudflare Tunnel); explicit localhost when developing locally.
const IS_LOCAL_DEV =
  typeof window !== "undefined" &&
  (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1");
const API_BASE_URL = IS_LOCAL_DEV ? "http://localhost:8080" : "";
const BOOKING_API_BASE_URL = IS_LOCAL_DEV ? "http://localhost:8081" : "";
const NOTIFICATION_API_BASE_URL = IS_LOCAL_DEV ? "http://localhost:8082" : "";
const DRIVER_STORAGE_KEY = "powersync.selectedVipDriverVin";
const selectedDriverVin = sessionStorage.getItem(DRIVER_STORAGE_KEY) ?? localStorage.getItem(DRIVER_STORAGE_KEY);
let selectedDriverLabel = selectedDriverVin ?? "-";
let previousSelectedDriverSnapshot = null;

if (!selectedDriverVin) {
  window.location.href = "./choose-driver.html";
}

function clearError() {
  errorBox.textContent = "";
}

function showError(message) {
  errorBox.textContent = message;
}

function showToast(message) {
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = message;
  toastContainer.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add("show"));
  setTimeout(() => {
    toast.classList.remove("show");
    setTimeout(() => toast.remove(), 220);
  }, 8500);
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

function candidateCarImagePaths(vin) {
  return [
    `./assets/cars/${vin}.png`,
    `./assets/cars/${vin}.jpg`,
    `./assets/cars/${vin}.jpeg`,
    `./assets/cars/${vin}.webp`,
    `./assets/cars/${vin.toLowerCase()}.png`,
    `./assets/cars/${vin.toLowerCase()}.jpg`,
    `./assets/cars/${vin.toLowerCase()}.jpeg`,
    `./assets/cars/${vin.toLowerCase()}.webp`
  ];
}

function resolveLocalCarImage(car) {
  return candidateCarImagePaths(car.vin)[0];
}

function stationLabel(stationId) {
  if (!stationId) return "None";
  return stationNamesById.get(String(stationId)) ?? stationId;
}

function renderCarStatusInto(statusBox, details) {
  const dotClass = carStatusDotClass(details.status);
  statusBox.innerHTML = `
    <strong>Status:</strong> <span class="status-dot ${dotClass}"></span>${details.status}<br>
    <strong>Battery:</strong> ${details.batteryPercentage}%<br>
    <strong>Station:</strong> ${stationLabel(details.assignedChargingStationId)}
  `;
}

function handleSelectedVipCompletion(car) {
  if (!selectedDriverVin || car.vin !== selectedDriverVin) {
    return;
  }
  const prev = previousSelectedDriverSnapshot;
  const reachedTargetAndReleased = car.batteryPercentage >= 80
    && car.status === "DRIVING"
    && !car.assignedChargingStationId;
  const wasChargingBefore = prev && prev.status === "CHARGING";
  if (reachedTargetAndReleased && wasChargingBefore) {
    showToast(`Your car reached ${car.batteryPercentage}%. Charging stopped and station released.`);
  }
  previousSelectedDriverSnapshot = {
    status: car.status,
    batteryPercentage: car.batteryPercentage,
    assignedChargingStationId: car.assignedChargingStationId
  };
}

function updateVipSelectionHint() {
  if (!selectedDriverVin) {
    vipSelectionHint.textContent = "You are booking as: -";
    bookVipButton.disabled = true;
    return;
  }
  vipSelectionHint.textContent = `You are booking as: ${selectedDriverLabel}`;
  bookVipButton.disabled = vipBookingInFlight;
}

function renderStationsStatus(stations) {
  stationsStatus.innerHTML = "";
  stationNamesById = new Map(
    stations
      .filter((station) => station.stationId && station.stationName)
      .map((station) => [String(station.stationId), station.stationName])
  );
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
    card.className = `station-card ${station.stationType === "VIP" ? "vip-station-card" : ""}`;
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
    const response = await fetch(`${NOTIFICATION_API_BASE_URL}/api/stations-status`);
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
  stationStatusEventSource = new EventSource(`${NOTIFICATION_API_BASE_URL}/api/stations-status/stream`);
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

function connectCarStatusStream() {
  if (carStatusEventSource) {
    carStatusEventSource.close();
  }
  carStatusEventSource = new EventSource(`${NOTIFICATION_API_BASE_URL}/api/cars-status/stream`);
  carStatusEventSource.addEventListener("car-status", (event) => {
    try {
      const cars = JSON.parse(event.data);
      for (const car of cars) {
        const statusBox = statusBoxByVin.get(car.vin);
        if (statusBox) {
          renderCarStatusInto(statusBox, car);
        }
        handleSelectedVipCompletion(car);
      }
    } catch (error) {
      showError(`Invalid car SSE payload: ${event.data}`);
    }
  });
  carStatusEventSource.onerror = () => {
    showError("SSE connection to notification-service (cars) interrupted.");
  };
}

async function loadInitialCarsStatus() {
  try {
    const response = await fetch(`${NOTIFICATION_API_BASE_URL}/api/cars-status`);
    if (!response.ok) {
      throw new Error(`Failed to load cars status (${response.status})`);
    }
    const cars = await response.json();
    for (const car of cars) {
      const statusBox = statusBoxByVin.get(car.vin);
      if (statusBox) {
        renderCarStatusInto(statusBox, car);
      }
      if (selectedDriverVin && car.vin === selectedDriverVin) {
        previousSelectedDriverSnapshot = {
          status: car.status,
          batteryPercentage: car.batteryPercentage,
          assignedChargingStationId: car.assignedChargingStationId
        };
      }
    }
  } catch (error) {
    showError(error.message);
  }
}

async function loadCars() {
  clearError();
  carsContainer.innerHTML = "Loading cars...";
  try {
    const response = await fetch(`${API_BASE_URL}/api/cars`);
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
  statusBoxByVin = new Map();
  for (const car of cars) {
    const fallbackImage = carImageDataUrl(car);
    const initialImage = resolveLocalCarImage(car);
    const card = document.createElement("article");
    card.className = `car-card ${car.vipEligible ? "vip-car-card" : ""}`;
    card.innerHTML = `
      <div class="car-image-wrap">
        <img class="car-image" alt="${car.name} illustration" src="${initialImage}" />
      </div>
      <div class="car-body">
        <div class="car-meta-row">
          <h3 class="car-title">${car.name}</h3>
          <span class="car-vin">VIN: ${car.vin}</span>
          <span class="car-vin">VIP: ${car.vipEligible ? "Yes" : "No"}</span>
        </div>
        <div class="car-status-row">
          <div class="status" id="status-${car.vin}">
            <strong>Status:</strong> <span class="status-dot gray"></span>Unknown<br>
            <strong>Battery:</strong> -<br>
            <strong>Station:</strong> -
          </div>
        </div>
      </div>
    `;

    const statusBox = card.querySelector(".status");
    const image = card.querySelector(".car-image");
    const candidates = candidateCarImagePaths(car.vin);
    let imageAttempt = 0;
    image.addEventListener("error", () => {
      imageAttempt += 1;
      if (imageAttempt < candidates.length) {
        image.src = candidates[imageAttempt];
        return;
      }
      image.onerror = null;
      image.src = fallbackImage;
    });
    statusBoxByVin.set(car.vin, statusBox);

    carsContainer.appendChild(card);
  }
}

function renderVipCars(vipCars) {
  const selectedCar = vipCars.find((car) => car.vin === selectedDriverVin);
  if (selectedCar) {
    selectedDriverLabel = `${selectedCar.name} (${selectedCar.vin})`;
    updateVipSelectionHint();
    return;
  }
  selectedDriverLabel = selectedDriverVin ?? "-";
  bookVipButton.disabled = true;
  showError(`Selected VIP car ${selectedDriverVin} is not available right now.`);
  updateVipSelectionHint();
}

async function bookVipStation() {
  clearError();
  if (vipBookingInFlight) {
    return;
  }
  const vin = selectedDriverVin;
  if (!vin) {
    showError("No VIP car selected.");
    return;
  }

  vipBookingInFlight = true;
  bookVipButton.disabled = true;
  vipResult.textContent = "Booking VIP station...";
  try {
    const bookResponse = await fetch(`${BOOKING_API_BASE_URL}/api/vip-stations/book?vin=${encodeURIComponent(vin)}`, {
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
  } finally {
    vipBookingInFlight = false;
    updateVipSelectionHint();
  }
}

bookVipButton.addEventListener("click", bookVipStation);
loadCars();
loadInitialStationsStatus();
loadInitialCarsStatus();
connectStationStream();
connectCarStatusStream();
