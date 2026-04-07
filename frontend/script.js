const apiBaseInput = document.getElementById("apiBaseUrl");
const loadCarsButton = document.getElementById("loadCarsButton");
const carsContainer = document.getElementById("carsContainer");
const errorBox = document.getElementById("error");

function clearError() {
  errorBox.textContent = "";
}

function showError(message) {
  errorBox.textContent = message;
}

function apiBaseUrl() {
  return apiBaseInput.value.trim().replace(/\/+$/, "");
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
      <button type="button">Get status</button>
      <div class="status" id="status-${car.vin}">No status loaded yet.</div>
    `;

    const button = card.querySelector("button");
    const statusBox = card.querySelector(".status");
    button.addEventListener("click", () => loadStatus(car.vin, statusBox));

    carsContainer.appendChild(card);
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

loadCarsButton.addEventListener("click", loadCars);
loadCars();
