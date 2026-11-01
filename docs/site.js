// Kleine Effekte für sinceus.de. Ohne JavaScript bleibt alles sichtbar und benutzbar.
(function () {
  var root = document.documentElement;
  var calm = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Navigation bekommt beim Scrollen einen Hintergrund
  var nav = document.querySelector(".nav");
  function onScroll() {
    if (nav) nav.classList.toggle("scrolled", window.scrollY > 24);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  // Einblenden beim Scrollen
  var targets = document.querySelectorAll(".section-head, main .card, .split > div, .help-note");
  if (calm || !("IntersectionObserver" in window)) return;
  var seen = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (e.isIntersecting) {
        e.target.classList.add("in");
        seen.unobserve(e.target);
      }
    });
  }, { rootMargin: "0px 0px -8% 0px" });
  targets.forEach(function (el) {
    el.classList.add("reveal");
    seen.observe(el);
  });

  // Die seitlichen Handys bewegen sich beim Scrollen leicht
  var p2 = document.querySelector(".phones .p2");
  var p1 = document.querySelector(".phones .p1");
  var ticking = false;
  function parallax() {
    var y = Math.min(window.scrollY, 800);
    if (p1) p1.style.transform = "translateY(" + y * -0.06 + "px)";
    if (p2) p2.style.transform = "translateY(" + y * 0.12 + "px)";
    ticking = false;
  }
  window.addEventListener("scroll", function () {
    if (!ticking) {
      ticking = true;
      window.requestAnimationFrame(parallax);
    }
  }, { passive: true });
})();
