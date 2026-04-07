const driverCards = [...document.querySelectorAll(".driver-card")];
const driverSelectionText = document.getElementById("driverSelectionText");
const enterDashboardButton = document.getElementById("enterDashboardButton");

const DRIVER_STORAGE_KEY = "powersync.selectedVipDriverVin";
let selectedVin = "7070";

function driverLabel(vin) {
  return vin === "8080" ? "ES90" : "EC40";
}

function renderSelectedCard() {
  for (const card of driverCards) {
    const isSelected = card.dataset.vin === selectedVin;
    card.classList.toggle("selected", isSelected);
  }
  driverSelectionText.textContent = `Selected: ${driverLabel(selectedVin)}`;
}

function preloadSelection() {
  renderSelectedCard();
}

function enterDashboard() {
  sessionStorage.setItem(DRIVER_STORAGE_KEY, selectedVin);
  localStorage.removeItem(DRIVER_STORAGE_KEY);
  window.location.href = "./index.html";
}

preloadSelection();
for (const card of driverCards) {
  card.addEventListener("click", () => {
    selectedVin = card.dataset.vin;
    renderSelectedCard();
  });
}
enterDashboardButton.addEventListener("click", enterDashboard);
