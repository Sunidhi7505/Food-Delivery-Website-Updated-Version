let allOrders = [];
let ordersState = [];
let ordersMap = null;
let ordersMarkers = [];
let orderRoute = null;
let riderMarker = null;
let riderInterval = null;

document.addEventListener("DOMContentLoaded", () => {
  if (!requireCustomer()) return;
  ["orderSearch", "orderStatusFilter", "orderPeriodFilter"].forEach((id) => {
    document.getElementById(id)?.addEventListener(id === "orderSearch" ? "input" : "change", renderOrders);
  });
  loadOrders();
  setInterval(loadOrders, 20000);
});

async function loadOrders() {
  try {
    allOrders = (await apiFetch(API.GET_ORDERS)) || [];
    renderOrders();
  } catch (error) {
    console.error(error);
  }
}

function filteredOrders() {
  const query = document.getElementById("orderSearch")?.value.trim().toLowerCase() || "";
  const status = document.getElementById("orderStatusFilter")?.value || "";
  const days = Number(document.getElementById("orderPeriodFilter")?.value || 0);
  const since = days ? Date.now() - days * 24 * 60 * 60 * 1000 : 0;

  return allOrders.filter((order) => {
    if (status === "ACTIVE" && ["DELIVERED", "CANCELLED"].includes(order.status)) return false;
    if (status && status !== "ACTIVE" && order.status !== status) return false;
    if (since && new Date(order.orderTime).getTime() < since) return false;
    if (!query) return true;
    return (
      (order.id || "").toLowerCase().endsWith(query) ||
      (order.items || []).some((item) => (item.name || "").toLowerCase().includes(query))
    );
  });
}

function renderOrders() {
  const ordersList = document.getElementById("ordersList");
  const emptyOrders = document.getElementById("emptyOrders");
  ordersList.innerHTML = "";

  if (allOrders.length === 0) {
    emptyOrders.style.display = "grid";
    document.getElementById("ordersMapPanel").style.display = "none";
    document.getElementById("orderFilters").style.display = "none";
    return;
  }

  emptyOrders.style.display = "none";
  document.getElementById("orderFilters").style.display = "flex";
  ordersState = filteredOrders();

  if (ordersState.length === 0) {
    ordersList.innerHTML = `<div class="empty"><div><h3>No orders match these filters</h3></div></div>`;
    document.getElementById("ordersMapPanel").style.display = "none";
    return;
  }

  document.getElementById("ordersMapPanel").style.display = "block";
  ordersState.forEach((order, index) => ordersList.appendChild(renderOrder(order, index)));

  const activeIndex = Math.max(ordersState.findIndex((order) => order.status !== "DELIVERED"), 0);
  showOrderMap(activeIndex);
  refreshIcons();
}

async function reorderOrder(orderId) {
  const result = await apiFetch(API.REORDER(orderId), { method: "POST" });
  if (!result) return;
  if (result.skipped?.length) {
    showToast(`Not available now: ${result.skipped.join(", ")}`, "error");
  }
  if (result.added?.length) {
    showToast(`Added to cart: ${result.added.join(", ")}`);
    setTimeout(() => (window.location.href = "cart.html"), 900);
  }
}

function renderOrder(order, index) {
  const card = document.createElement("article");
  card.className = "order-card card";
  const statusTone = order.status === "DELIVERED" ? "success" : order.status === "CANCELLED" ? "danger" : "warning";
  const items = order.items || [];

  card.innerHTML = `
    <div class="card-row">
      <div>
        <p class="eyebrow">Order ${escapeHtml(order.id?.slice(-6) || "")}</p>
        <h3>${items.length} item${items.length === 1 ? "" : "s"} - ${money(order.totalAmount)}</h3>
        <p class="muted">${formatDateTime(order.orderTime)}</p>
      </div>
      <span class="pill pill-${statusTone}">${statusText(order.status)}</span>
    </div>
    <div class="status-timeline">${statusTimeline(order.status)}</div>
    <div class="panel" style="box-shadow:none;">
      ${items
        .map(
          (item) => `
          <div class="card-row">
            <span>${escapeHtml(item.name)} x ${item.quantity}</span>
            <strong>${money(item.price * item.quantity)}</strong>
          </div>`
        )
        .join("")}
      ${order.discountAmount ? `<div class="card-row muted"><span>Discount</span><span>- ${money(order.discountAmount)}</span></div>` : ""}
      ${order.taxAmount ? `<div class="card-row muted"><span>GST</span><span>${money(order.taxAmount)}</span></div>` : ""}
      ${order.deliveryFee ? `<div class="card-row muted"><span>Delivery fee</span><span>${money(order.deliveryFee)}</span></div>` : ""}
    </div>
    <div class="hero-actions">
      <button class="btn btn-outline" type="button" onclick="showOrderMap(${index})">
        <i data-lucide="map"></i>Show route
      </button>
      <button class="btn btn-outline" type="button" onclick="reorderOrder('${order.id}')">
        <i data-lucide="repeat"></i>Reorder
      </button>
      <button class="btn btn-outline" type="button" onclick="printInvoice('${order.id}')">
        <i data-lucide="receipt-text"></i>Invoice
      </button>
      <button class="btn btn-ghost" type="button" onclick="downloadInvoice('${order.id}')">
        <i data-lucide="download"></i>Download
      </button>
      ${
        canCancel(order)
          ? `<button class="btn btn-danger" type="button" onclick="cancelOrder('${order.id}')"><i data-lucide="x"></i>Cancel</button>`
          : ""
      }
      ${
        order.status === "DELIVERED"
          ? items
              .map(
                (item) =>
                  `<button class="btn btn-primary" type="button" onclick="location.href='product.html?id=${item.productId}'"><i data-lucide="message-square"></i>Review ${escapeHtml(item.name)}</button>`
              )
              .join("")
          : ""
      }
    </div>
  `;

  return card;
}

function statusTimeline(status) {
  const currentIndex = ORDER_STATUSES.indexOf(status);
  return ORDER_STATUSES.map((step, index) => {
    const done = currentIndex >= index;
    return `
      <div class="status-step ${done ? "done" : ""}">
        <span class="status-dot"></span>
        <span>${statusText(step)}</span>
      </div>
    `;
  }).join("");
}

function statusText(status) {
  const labels = {
    PLACED: "Placed",
    CONFIRMED: "Confirmed",
    PREPARING: "Preparing",
    OUT_FOR_DELIVERY: "Rider on the way",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
  };
  return labels[status] || status;
}

function showOrderMap(index) {
  const order = ordersState[index];
  const mapEl = document.getElementById("ordersMap");
  if (!order || !mapEl || !window.L) return;

  const restaurant = order.restaurant;
  const address = order.deliveryAddress;
  const restaurantCoords = [
    restaurant?.latitude || 28.6139,
    restaurant?.longitude || 77.209,
  ];
  const addressCoords = address?.latitude && address?.longitude ? [address.latitude, address.longitude] : null;

  if (!ordersMap) {
    ordersMap = L.map("ordersMap", { scrollWheelZoom: false }).setView(restaurantCoords, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap",
    }).addTo(ordersMap);
  }

  clearRiderAnimation();
  ordersMarkers.forEach((marker) => marker.remove());
  if (orderRoute) {
    orderRoute.remove();
    orderRoute = null;
  }
  if (riderMarker) {
    riderMarker.remove();
    riderMarker = null;
  }

  ordersMarkers = [L.marker(restaurantCoords).addTo(ordersMap).bindPopup("Restaurant")];

  if (addressCoords) {
    ordersMarkers.push(L.marker(addressCoords).addTo(ordersMap).bindPopup("Delivery address"));
    orderRoute = L.polyline([restaurantCoords, addressCoords], {
      color: "#1f9d55",
      weight: 5,
      opacity: 0.9,
    }).addTo(ordersMap);
    riderMarker = L.circleMarker(riderPosition(order.status, restaurantCoords, addressCoords, 0), {
      radius: 9,
      color: "#ffffff",
      weight: 3,
      fillColor: "#1f9d55",
      fillOpacity: 1,
    }).addTo(ordersMap).bindPopup("Rider");
    animateRider(order.status, restaurantCoords, addressCoords);
    ordersMap.fitBounds([restaurantCoords, addressCoords], { padding: [34, 34] });
  } else {
    ordersMap.setView(restaurantCoords, 12);
  }

  const title = document.getElementById("ordersMapTitle");
  if (title) title.textContent = `Tracking order ${order.id?.slice(-6) || ""}`;
  const eta = document.getElementById("ordersEta");
  if (eta && addressCoords) {
    eta.style.display = "block";
    eta.textContent = trackingMessage(order.status, restaurantCoords, addressCoords);
  }
  setTimeout(() => ordersMap.invalidateSize(), 100);
}

function canCancel(order) {
  return order.status === "PLACED" || order.status === "CONFIRMED";
}

async function cancelOrder(orderId) {
  if (!confirm("Cancel this order?")) return;
  await apiFetch(API.CANCEL_ORDER(orderId), { method: "PUT" });
  showToast("Order cancelled and stock restored.");
  loadOrders();
}

async function printInvoice(orderId) {
  const invoice = await apiFetch(API.ORDER_INVOICE(orderId));
  const win = window.open("", "_blank", "width=720,height=800");
  win.document.write(`<pre style="font:16px/1.5 monospace;white-space:pre-wrap;">${escapeHtml(invoice)}</pre>`);
  win.document.close();
  win.print();
}

async function downloadInvoice(orderId) {
  const invoice = await apiFetch(API.ORDER_INVOICE(orderId));
  const blob = new Blob([invoice], { type: "text/plain" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `foody-invoice-${orderId.slice(-6)}.txt`;
  link.click();
  URL.revokeObjectURL(link.href);
}

function animateRider(status, start, end) {
  if (!riderMarker || status !== "OUT_FOR_DELIVERY") return;
  let progress = 0;
  riderInterval = setInterval(() => {
    progress += 0.035;
    if (progress > 1) progress = 0;
    riderMarker.setLatLng(riderPosition(status, start, end, progress));
  }, 900);
}

function clearRiderAnimation() {
  if (riderInterval) {
    clearInterval(riderInterval);
    riderInterval = null;
  }
}

function riderPosition(status, start, end, progress) {
  let ratio = 0;
  if (status === "OUT_FOR_DELIVERY") ratio = progress;
  if (status === "DELIVERED") ratio = 1;
  return [
    start[0] + (end[0] - start[0]) * ratio,
    start[1] + (end[1] - start[1]) * ratio,
  ];
}

function trackingMessage(status, start, end) {
  if (status === "DELIVERED") return "Delivered at your address.";
  if (status === "OUT_FOR_DELIVERY") return `Rider moving on green route. ETA around ${etaMinutes(start, end)} min.`;
  if (status === "CANCELLED") return "Order cancelled.";
  return "Restaurant and delivery address are fixed. Rider starts after the order is out for delivery.";
}

function routeDistanceKm(start, end) {
  const toRad = (value) => (value * Math.PI) / 180;
  const earthRadiusKm = 6371;
  const dLat = toRad(end[0] - start[0]);
  const dLng = toRad(end[1] - start[1]);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(start[0])) * Math.cos(toRad(end[0])) *
      Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function etaMinutes(start, end) {
  return Math.max(8, Math.round((routeDistanceKm(start, end) / 18) * 60));
}
