const apiBaseInput = document.getElementById("apiBaseUrl");
const bookingApiBaseInput = document.getElementById("bookingApiBaseUrl");
const notificationApiBaseInput = document.getElementById("notificationApiBaseUrl");
const loadCarsButton = document.getElementById("loadCarsButton");
const vipCarSelect = document.getElementById("vipCarSelect");
const bookVipButton = document.getElementById("bookVipButton");
const vipStationStatus = document.getElementById("vipStationStatus");
const vipResult = document.getElementById("vipResult");
const carsContainer = document.getElementById("carsContainer");
const errorBox = document.getElementById("error");
let vipStatusEventSource = null;

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

function renderVipStationStatus(payload) {
  const status = payload?.status ?? "UNKNOWN";
  const assignedVin = payload?.assignedVin ?? "None";
  vipStationStatus.innerHTML = `
    <strong>VIP station status:</strong> ${status}<br>
    <strong>Assigned VIN:</strong> ${assignedVin}
  `;
}

async function loadInitialVipStationStatus() {
  try {
    const response = await fetch(`${notificationApiBaseUrl()}/api/vip-station-status`);
    if (!response.ok) {
      throw new Error(`Failed to load VIP station status (${response.status})`);
    }
    renderVipStationStatus(await response.json());
  } catch (error) {
    showError(error.message);
  }
}

function connectVipStationStream() {
  if (vipStatusEventSource) {
    vipStatusEventSource.close();
  }
  vipStatusEventSource = new EventSource(`${notificationApiBaseUrl()}/api/vip-station-status/stream`);
  vipStatusEventSource.addEventListener("vip-station-status", (event) => {
    try {
      renderVipStationStatus(JSON.parse(event.data));
    } catch (error) {
      showError(`Invalid SSE payload: ${event.data}`);
    }
  });
  vipStatusEventSource.onerror = () => {
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
      <h3 class="car-title">${car.name}</h3>
      <p class="car-vin">VIN: ${car.vin}</p>
      <p class="car-vin">VIP eligible: ${car.vipEligible ? "Yes" : "No"}</p>
      <button type="button">Get status</button>
      <div class="status" id="status-${car.vin}">No status loaded yet.</div>
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
    statusBox.innerHTML = `
      <strong>Status:</strong> ${details.status}<br>
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
loadInitialVipStationStatus();
connectVipStationStream();
