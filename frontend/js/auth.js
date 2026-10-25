document.addEventListener("DOMContentLoaded", () => {
  if (isLoggedIn()) {
    window.location.href = "index.html";
    return;
  }

  document.getElementById("loginForm")?.addEventListener("submit", login);
  document.getElementById("registerForm")?.addEventListener("submit", register);
  document.getElementById("forgotForm")?.addEventListener("submit", forgotPassword);
  document.getElementById("resetForm")?.addEventListener("submit", resetPassword);
});

async function login(event) {
  event.preventDefault();
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!email || !password) {
    showToast("Please fill all fields.", "error");
    return;
  }

  const data = await apiFetch(API.LOGIN, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  localStorage.setItem("user", JSON.stringify(data));
  showToast("Login successful.");
  window.location.href = data.role === "ROLE_ADMIN" ? "admin.html" : "index.html";
}

async function register(event) {
  event.preventDefault();
  const name = document.getElementById("name").value.trim();
  const email = document.getElementById("email").value.trim();
  const phone = document.getElementById("phone").value.trim();
  const password = document.getElementById("password").value.trim();
  const confirmPassword = document.getElementById("confirmPassword").value.trim();

  if (!name || !email || !password || !confirmPassword) {
    showToast("Please fill all required fields.", "error");
    return;
  }

  if (password.length < 6) {
    showToast("Password must be at least 6 characters.", "error");
    return;
  }

  if (password !== confirmPassword) {
    showToast("Passwords do not match.", "error");
    return;
  }

  await apiFetch(API.REGISTER, {
    method: "POST",
    body: JSON.stringify({ name, email, phone, password }),
  });

  showToast("Registration successful. Please login.");
  setTimeout(() => {
    window.location.href = "login.html";
  }, 700);
}

async function forgotPassword(event) {
  event.preventDefault();
  const email = document.getElementById("email").value.trim();
  if (!email) return;

  const data = await apiFetch(API.FORGOT_PASSWORD, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
  showToast(data?.message || "Check your email for the reset link.");
}

async function resetPassword(event) {
  event.preventDefault();
  const token = new URLSearchParams(window.location.search).get("token");
  const password = document.getElementById("password").value;
  const confirmPassword = document.getElementById("confirmPassword").value;

  if (!token) {
    showToast("This reset link is invalid. Request a new one.", "error");
    return;
  }
  if (password !== confirmPassword) {
    showToast("Passwords do not match.", "error");
    return;
  }

  await apiFetch(API.RESET_PASSWORD, {
    method: "POST",
    body: JSON.stringify({ token, newPassword: password }),
  });
  showToast("Password updated. Please login.");
  setTimeout(() => (window.location.href = "login.html"), 900);
}
