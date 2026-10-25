let selectedCategory = "";
let wishlistIds = [];

document.addEventListener("DOMContentLoaded", async () => {
  document.getElementById("heroCta")?.addEventListener("click", () => {
    document.getElementById("menuSection")?.scrollIntoView({ behavior: "smooth" });
  });
  document.getElementById("catPrev")?.addEventListener("click", () => scrollCategories(-1));
  document.getElementById("catNext")?.addEventListener("click", () => scrollCategories(1));
  document.getElementById("featuredPrev")?.addEventListener("click", () => scrollFeatured(-1));
  document.getElementById("featuredNext")?.addEventListener("click", () => scrollFeatured(1));
  await loadWishlist();
  loadCategories();
  loadFeatured();
  loadRestaurantStatus();
  loadProducts();
});

async function loadWishlist() {
  const user = getUser();
  if (user?.role !== "ROLE_CUSTOMER") return;
  try {
    wishlistIds = (await apiFetch(API.WISHLIST)) || [];
  } catch (error) {
    wishlistIds = [];
  }
}

async function loadFeatured() {
  const track = document.getElementById("featuredTrack");
  if (!track) return;

  try {
    const products = await apiFetch(API.FEATURED_PRODUCTS);
    track.innerHTML = "";
    (products || []).forEach((product) => track.appendChild(renderProductCard(product)));
    refreshIcons();
  } catch (error) {
    track.innerHTML = "";
  }
}

async function loadCategories() {
  const track = document.getElementById("categoryTrack");
  if (!track) return;

  try {
    const categories = await apiFetch(API.PRODUCT_CATEGORIES);
    const allButton = categoryButton("All dishes", "", true);
    track.innerHTML = allButton + (categories || []).map((category) => categoryButton(category, category)).join("");
    refreshIcons();
  } catch (error) {
    track.innerHTML = categoryButton("All dishes", "", true);
  }
}

function categoryButton(label, value, active = false) {
  return `<button class="chip ${active ? "active" : ""}" type="button" onclick="selectCategory(decodeURIComponent('${encodeURIComponent(value)}'), this)">${escapeHtml(label)}</button>`;
}

function selectCategory(category, button) {
  selectedCategory = category;
  document.querySelectorAll(".category-track .chip").forEach((chip) => chip.classList.remove("active"));
  button.classList.add("active");
  applyFilters();
}

function scrollCategories(direction) {
  document.getElementById("categoryTrack")?.scrollBy({
    left: direction * 260,
    behavior: "smooth",
  });
}

function scrollFeatured(direction) {
  document.getElementById("featuredTrack")?.scrollBy({
    left: direction * 340,
    behavior: "smooth",
  });
}

const PAGE_SIZE = 12;
let currentPage = 0;

function filterParams() {
  const params = new URLSearchParams();
  const keyword = document.getElementById("searchKeyword").value.trim();
  const min = document.getElementById("minPrice").value;
  const max = document.getElementById("maxPrice").value;
  const available = document.getElementById("availability").value;
  const sort = document.getElementById("sortBy").value;
  const direction = document.getElementById("direction").value;
  const vegetarian = document.getElementById("vegOnly").checked;

  if (keyword) params.append("keyword", keyword);
  if (min) params.append("minPrice", min);
  if (max) params.append("maxPrice", max);
  if (available !== "") params.append("available", available);
  if (sort) params.append("sortBy", sort);
  if (direction) params.append("direction", direction);
  if (selectedCategory) params.append("category", selectedCategory);
  if (vegetarian) params.append("vegetarian", "true");
  return params;
}

async function applyFilters() {
  await loadProducts(true);
}

// Server-side pagination: first page replaces the grid, "Load more" appends the next one.
async function loadProducts(reset = true) {
  const grid = document.getElementById("productGrid");
  const emptyState = document.getElementById("emptyState");
  const moreButton = document.getElementById("loadMoreBtn");
  if (!grid) return;

  if (reset) {
    currentPage = 0;
    grid.innerHTML = Array.from({ length: 6 })
      .map(() => `<div class="product-card skeleton"></div>`)
      .join("");
  }

  try {
    const params = filterParams();
    params.append("page", currentPage);
    params.append("size", PAGE_SIZE);
    const data = await apiFetch(`${API.PRODUCT_PAGE}?${params}`);
    if (reset) grid.innerHTML = "";

    const products = data?.items || [];
    emptyState.style.display = reset && products.length === 0 ? "grid" : "none";
    products.forEach((product) => grid.appendChild(renderProductCard(product)));

    if (moreButton) {
      moreButton.style.display = data?.hasMore ? "inline-flex" : "none";
      moreButton.onclick = async () => {
        currentPage += 1;
        moreButton.disabled = true;
        await loadProducts(false);
        moreButton.disabled = false;
      };
    }
    refreshIcons();
  } catch (error) {
    if (reset) grid.innerHTML = "";
    emptyState.style.display = reset ? "grid" : "none";
  }
}

async function loadRestaurantStatus() {
  const pill = document.getElementById("restaurantStatus");
  if (!pill) return;
  try {
    const status = await apiFetch(API.RESTAURANT_STATUS);
    pill.className = status.open ? "status-pill-open" : "status-pill-closed";
    pill.textContent = status.open ? "Open now" : `Closed - opens ${status.openingHours.split("-")[0].trim()}`;
  } catch (error) {
    pill.style.display = "none";
  }
}

function renderProductCard(product) {
  const user = getUser();
  const isAdmin = user?.role === "ROLE_ADMIN";
  const stock = stockLabel(product);
  const out = !product.available || product.stock <= 0;
  const card = document.createElement("article");
  card.className = "product-card";

  card.innerHTML = `
    <div class="product-media">
      <img src="${imageOrFallback(product.imageUrl)}" alt="${escapeHtml(product.name)}" onerror="this.src='${FALLBACK_IMAGE}'" />
      ${
        !isAdmin
          ? `<button class="wishlist-button ${wishlistIds.includes(product.id) ? "active" : ""}" type="button" onclick="toggleWishlist(event, '${product.id}')" aria-label="Wishlist">
              <i data-lucide="heart"></i>
            </button>`
          : ""
      }
    </div>
    <div class="product-body">
      <div class="card-row">
        <span class="pill">${escapeHtml(product.category || "Meals")}</span>
        <span class="pill pill-${stock.tone}">${stock.text}</span>
      </div>
      <h3>${escapeHtml(product.name)}</h3>
      ${ratingLine(product)}
      <p>${escapeHtml(product.description || "Freshly prepared and packed for delivery.")}</p>
      <div class="product-meta">
        <span class="price">${money(product.price)}</span>
        <span class="muted">${product.preparationTimeMinutes || 25} min</span>
      </div>
      <div class="hero-actions">
        <button class="btn btn-outline" type="button" onclick="viewProduct('${product.id}')">
          <i data-lucide="eye"></i>Details
        </button>
        ${
          isAdmin
            ? ""
            : `<button class="btn btn-primary" type="button" ${out ? "disabled" : ""} onclick="addToCart('${product.id}')">
                <i data-lucide="shopping-cart"></i>${out ? "Unavailable" : "Add"}
              </button>`
        }
      </div>
    </div>
  `;

  return card;
}

function ratingLine(product) {
  const count = product.reviewCount || 0;
  const average = product.averageRating || 0;
  return `
    <div class="rating-line">
      <strong>${average ? average.toFixed(1) : "New"}</strong>
      <span>${count ? `${count} review${count === 1 ? "" : "s"}` : "No reviews yet"}</span>
    </div>
  `;
}

function viewProduct(productId) {
  window.location.href = `product.html?id=${productId}`;
}

async function addToCart(productId) {
  const user = getUser();
  if (!user) {
    showToast("Please login first.", "error");
    window.location.href = "login.html";
    return;
  }

  if (user.role === "ROLE_ADMIN") {
    showToast("Admins can manage products from the dashboard.", "error");
    return;
  }

  await apiFetch(API.ADD_TO_CART(productId, 1), { method: "POST" });
  showToast("Added to cart.");
  updateCartCount();
}

async function toggleWishlist(event, productId) {
  event.stopPropagation();
  const user = getUser();
  if (!user) {
    showToast("Please login first.", "error");
    window.location.href = "login.html";
    return;
  }

  if (wishlistIds.includes(productId)) {
    wishlistIds = await apiFetch(API.WISHLIST_ITEM(productId), { method: "DELETE" });
    showToast("Removed from wishlist.");
  } else {
    wishlistIds = await apiFetch(API.WISHLIST_ITEM(productId), { method: "POST" });
    showToast("Saved to wishlist.");
  }

  document.querySelectorAll(`.wishlist-button`).forEach((button) => {
    const onclick = button.getAttribute("onclick") || "";
    if (onclick.includes(productId)) {
      button.classList.toggle("active", wishlistIds.includes(productId));
    }
  });
  refreshIcons();
}
