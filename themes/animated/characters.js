/**
 * Animated Characters for Login/Register/Reset pages
 * Based on sparkai-animated-login (Vue) — ported to vanilla JS
 *
 * Features:
 *   - Mouse tracking (eyes, body tilt, pupils)
 *   - Independent blinking for all 4 characters
 *   - Password focus: characters cover eyes / hide
 *   - Password reveal: purple peeks periodically
 *   - Login error: sad expressions (inverted mouths, worried eyes, distorted shapes)
 *   - Input focus: brief "look at each other" transition
 *   - Orange squash & stretch
 *   - Black blush (pink cheeks)
 *   - Entrance animation (bounce in from bottom)
 *   - Orange glasses properly aligned
 */
(function () {
  'use strict';

  // ===== Config =====
  var CFG = {
    mouseSensX: 20,
    mouseSensY: 30,
    bodySkewSens: 120,
    pupilFollow: 0.35,
    blinkMin: 2500,
    blinkRange: 3500,
    blinkDur: 140,
    transition: 0.7,
    errorRecover: 2500,
    peekMin: 2000,
    peekRange: 3000
  };

  // ===== State =====
  var mouseX = 0, mouseY = 0;
  var focusedField = 'none'; // 'none' | 'username' | 'password'
  var isPasswordVisible = false;
  var passwordLength = 0;
  var isTyping = false;
  var isLoginError = false;
  var internalError = false;
  var isErrorLocked = false;

  var isPurpleBlinking = false, isBlackBlinking = false;
  var isOrangeBlinking = false, isYellowBlinking = false;
  var isPurplePeeking = false;
  var isLookingAtEachOther = false;
  var entranceDone = false;

  var purpleBlinkTimer, blackBlinkTimer, orangeBlinkTimer, yellowBlinkTimer;
  var purplePeekTimer, lookingTimer, errorTimer;

  // ===== DOM refs =====
  var container = document.querySelector('.characters-container');
  var els = {
    purple: document.getElementById('purple'),
    black: document.getElementById('black'),
    orange: document.getElementById('orange'),
    yellow: document.getElementById('yellow'),
    purpleEyes: document.getElementById('purple-eyes'),
    blackEyes: document.getElementById('black-eyes'),
    orangeEyes: document.getElementById('orange-eyes'),
    yellowEyes: document.getElementById('yellow-eyes'),
    purpleMouth: document.getElementById('purple-mouth'),
    orangeMouth: document.getElementById('orange-mouth'),
    yellowMouth: document.getElementById('yellow-mouth'),
    blackBlushL: document.getElementById('black-blush-l'),
    blackBlushR: document.getElementById('black-blush-r'),
    glasses: document.getElementById('orange-glasses')
  };

  var allEyeballs = [
    { id: 'pe1', maxDist: 5, char: 'purple' },
    { id: 'pe2', maxDist: 5, char: 'purple' },
    { id: 'be1', maxDist: 4, char: 'black' },
    { id: 'be2', maxDist: 4, char: 'black' },
    { id: 'oe1', maxDist: 5, char: 'orange' },
    { id: 'oe2', maxDist: 5, char: 'orange' },
    { id: 'ye1', maxDist: 5, char: 'yellow' },
    { id: 'ye2', maxDist: 5, char: 'yellow' }
  ];

  function clamp(v, min, max) { return Math.max(min, Math.min(max, v)); }

  // ===== Mouse offset from container center =====
  function getOffset() {
    if (!container) return { faceX: 0, faceY: 0, bodySkew: 0 };
    var r = container.getBoundingClientRect();
    var cx = r.left + r.width / 2;
    var cy = r.top + r.height / 3;
    var dx = mouseX - cx;
    var dy = mouseY - cy;
    return {
      faceX: clamp(dx / CFG.mouseSensX, -15, 15),
      faceY: clamp(dy / CFG.mouseSensY, -10, 10),
      bodySkew: clamp(-dx / CFG.bodySkewSens, -6, 6)
    };
  }

  // ===== Derived states =====
  function isRevealState() { return passwordLength > 0 && isPasswordVisible; }
  function isPasswordFocused() { return focusedField === 'password'; }
  function isNonPasswordFocused() { return focusedField !== 'password' && focusedField !== 'none'; }
  function isTiltState() { return isNonPasswordFocused() || (passwordLength > 0 && !isPasswordVisible && isPasswordFocused()); }
  function isHiding() { return isPasswordFocused() && !isPasswordVisible; }

  // ===== Apply styles helper =====
  function css(el, props) {
    if (!el) return;
    for (var k in props) el.style[k] = props[k];
  }

  // ===== Update all characters =====
  function tick() {
    var o = getOffset();
    var fx = o.faceX, fy = o.faceY, bs = o.bodySkew;

    // --- PURPLE ---
    if (internalError && !isPasswordVisible) {
      css(els.purple, { transform: 'translateX(0px)', height: '440px', clipPath: 'polygon(30% 6%, 85% 0%, 70% 42%, 78% 100%, 22% 100%, 15% 44%)' });
      css(els.purpleEyes, { left: (100 + fx) + 'px', top: (38 + fy) + 'px' });
    } else if (isRevealState()) {
      css(els.purple, { transform: 'skewX(0deg)', height: isNonPasswordFocused() ? '460px' : '420px', clipPath: 'none' });
      css(els.purpleEyes, { left: '65px', top: '35px' });
    } else if (isHiding()) {
      css(els.purple, { transform: 'skewX(0deg)', height: '440px', clipPath: 'none' });
      css(els.purpleEyes, { left: '20px', top: '35px' });
    } else if (isTiltState()) {
      css(els.purple, { transform: 'skewX(' + (bs - 12) + 'deg) translateX(40px)', height: '460px', clipPath: 'none' });
      css(els.purpleEyes, { left: (80 + fx) + 'px', top: (65 + fy) + 'px' });
    } else if (isLookingAtEachOther) {
      css(els.purple, { transform: 'skewX(' + (bs - 12) + 'deg) translateX(40px)', height: '420px', clipPath: 'none' });
      css(els.purpleEyes, { left: '80px', top: '65px' });
    } else {
      css(els.purple, { transform: 'skewX(' + bs + 'deg)', height: '420px', clipPath: 'none' });
      css(els.purpleEyes, { left: (78 + fx) + 'px', top: (40 + fy) + 'px' });
    }

    // Purple mouth
    if (els.purpleMouth) {
      if (internalError) {
        css(els.purpleMouth, { width: '26px', height: '13px', borderRadius: '0 0 50% 50%', background: 'transparent', borderTop: 'none', borderBottom: '3.5px solid #2D2D2D', left: (105 + fx) + 'px', top: (78 + fy) + 'px', opacity: '1' });
      } else if (isTiltState() || isHiding()) {
        css(els.purpleMouth, { width: '6px', height: '14px', borderRadius: '3px', background: '#2D2D2D', borderTop: 'none', borderBottom: 'none', left: (90 + fx) + 'px', top: (82 + fy) + 'px', opacity: '1' });
      } else {
        css(els.purpleMouth, { width: '16px', height: '4px', borderRadius: '2px', background: '#2D2D2D', borderTop: 'none', borderBottom: 'none', left: (90 + fx) + 'px', top: (80 + fy) + 'px', opacity: '1' });
      }
    }

    // --- BLACK ---
    if (internalError && !isPasswordVisible) {
      css(els.black, { transform: 'skewX(0deg)' });
      css(els.blackEyes, { left: (26 + fx) + 'px', top: (32 + fy) + 'px' });
    } else if (isRevealState()) {
      css(els.black, { transform: 'skewX(0deg)' });
      css(els.blackEyes, { left: '20px', top: '28px' });
    } else if (isHiding()) {
      css(els.black, { transform: 'skewX(0deg)' });
      css(els.blackEyes, { left: '10px', top: '28px' });
    } else if (isLookingAtEachOther) {
      css(els.black, { transform: 'skewX(' + (bs * 1.5 + 10) + 'deg) translateX(15px)' });
      css(els.blackEyes, { left: '42px', top: '12px' });
    } else {
      css(els.black, { transform: 'skewX(' + (bs * 1.5) + 'deg)' });
      css(els.blackEyes, { left: (26 + fx) + 'px', top: (32 + fy) + 'px' });
    }

    // Black blush
    if (els.blackBlushL && els.blackBlushR) {
      var blushSize = isHiding() ? 28 : (internalError ? 22 : 18);
      var blushOpacity = isHiding() ? 0.9 : (internalError ? 0.75 : 0.55);
      var bx, by;
      if (isRevealState()) { bx = 44; by = 52; }
      else if (isLookingAtEachOther) { bx = 66; by = 36; }
      else { bx = 50 + fx; by = 56 + fy; }
      css(els.blackBlushL, { left: (bx - 22) + 'px', top: by + 'px', width: blushSize + 'px', height: Math.round(blushSize * 0.65) + 'px', opacity: blushOpacity });
      css(els.blackBlushR, { left: (bx + 22) + 'px', top: by + 'px', width: blushSize + 'px', height: Math.round(blushSize * 0.65) + 'px', opacity: blushOpacity });
    }

    // --- ORANGE (squash & stretch) ---
    if (internalError && !isPasswordVisible) {
      css(els.orange, { transform: 'skewX(-4deg) scaleX(0.96) scaleY(0.92)', borderRadius: '92px 166px 0 0' });
      css(els.orangeEyes, { left: (95 + fx * 1.8) + 'px', top: (90 + fy * 1.3) + 'px' });
    } else if (isRevealState() || isHiding()) {
      css(els.orange, { transform: 'skewX(0deg) scaleX(1) scaleY(1)', borderRadius: '120px 120px 0 0' });
      css(els.orangeEyes, { left: (75 + fx * 0.7) + 'px', top: (88 + fy * 0.6) + 'px' });
    } else {
      var squash = fy * 0.011;
      var pull = Math.abs(fx) * 0.004;
      var sx = 1 + squash + pull;
      var sy = 1 - squash - pull * 0.45;
      var skew = -fx * 0.56;
      var lr = clamp(138 + fx * 3.5 + fy * 1.1, 100, 200);
      var rr = clamp(138 - fx * 3.5 + fy * 1.1, 100, 200);
      css(els.orange, { transform: 'skewX(' + skew.toFixed(2) + 'deg) scaleX(' + sx.toFixed(3) + ') scaleY(' + sy.toFixed(3) + ')', borderRadius: lr + 'px ' + rr + 'px 0 0' });
      css(els.orangeEyes, { left: (85 + fx * 2.4) + 'px', top: (90 + fy * 1.45) + 'px' });
    }

    // Orange mouth
    if (els.orangeMouth) {
      if (internalError) {
        css(els.orangeMouth, { width: '24px', height: '12px', borderRadius: '0 0 50% 50%', background: 'transparent', borderTop: 'none', borderBottom: '3px solid #2D2D2D', left: (113 + fx * 1.8) + 'px', top: (118 + fy * 1.3) + 'px' });
      } else if (isHiding()) {
        css(els.orangeMouth, { width: '12px', height: '12px', borderRadius: '50%', background: '#2D2D2D', borderTop: 'none', borderBottom: 'none', left: (113 + fx * 2.1) + 'px', top: (117 + fy * 1.55) + 'px' });
      } else {
        css(els.orangeMouth, { width: '24px', height: '12px', borderRadius: '0 0 24px 24px', background: '#2D2D2D', borderTop: 'none', borderBottom: 'none', left: (113 + fx * 2.2) + 'px', top: (117 + fy * 1.6) + 'px' });
      }
    }

    // Orange glasses follow eyes
    if (els.glasses && els.orangeEyes) {
      var gLeft = parseFloat(els.orangeEyes.style.left) - 14;
      var gTop = parseFloat(els.orangeEyes.style.top) - 8;
      var tilt = 3 + fx * 0.25;
      css(els.glasses, { left: gLeft + 'px', top: gTop + 'px', transform: 'rotate(' + tilt.toFixed(2) + 'deg)' });
    }

    // --- YELLOW ---
    if (isRevealState() || isHiding()) {
      css(els.yellow, { transform: 'skewX(0deg)' });
      css(els.yellowEyes, { left: '22px', top: '39px' });
    } else {
      css(els.yellow, { transform: 'skewX(' + bs + 'deg)' });
      css(els.yellowEyes, { left: (52 + fx) + 'px', top: (40 + fy) + 'px' });
    }

    // Yellow mouth
    if (els.yellowMouth) {
      if (internalError && !isPasswordVisible) {
        css(els.yellowMouth, { width: '50px', height: '10px', borderRadius: '0 0 50% 50%', background: 'transparent', borderTop: 'none', borderBottom: '3px solid #2D2D2D', left: '50%', top: '98px', transform: 'translateX(-50%) translate(' + (fx * 0.5) + 'px,' + (fy * 0.5) + 'px)' });
      } else {
        css(els.yellowMouth, { width: '80px', height: '4px', borderRadius: '4px', background: '#2D2D2D', borderTop: 'none', borderBottom: 'none', left: (40 + fx) + 'px', top: (88 + fy) + 'px', transform: 'none' });
      }
    }

    // --- PUPILS ---
    updatePupils(fx, fy);

    requestAnimationFrame(tick);
  }

  // ===== Pupil update =====
  function updatePupils(fx, fy) {
    var pf = CFG.pupilFollow;
    allEyeballs.forEach(function (eb) {
      var el = document.getElementById(eb.id);
      if (!el) return;
      var pupil = el.querySelector('.pupil');
      if (!pupil) return;
      var tx, ty;

      if (internalError && !isPasswordVisible) {
        tx = (eb.char === 'purple') ? 0 : (fx * pf);
        ty = 5;
      } else if (isRevealState()) {
        if (eb.char === 'purple') {
          tx = isPurplePeeking ? 4 : -4;
          ty = isPurplePeeking ? 5 : -4;
        } else {
          tx = -4; ty = -4;
        }
      } else if (isHiding()) {
        if (eb.char === 'purple' || eb.char === 'black') {
          tx = -4; ty = -4;
        } else {
          tx = fx * pf; ty = fy * pf;
        }
      } else if (isLookingAtEachOther) {
        if (eb.char === 'purple') { tx = 3; ty = 4; }
        else if (eb.char === 'black') { tx = 0; ty = -4; }
        else { tx = fx * pf; ty = fy * pf; }
      } else {
        tx = fx * pf; ty = fy * pf;
      }

      pupil.style.transform = 'translate3d(' + tx.toFixed(1) + 'px,' + ty.toFixed(1) + 'px,0)';
    });
  }

  // ===== Blinking =====
  function scheduleBlink(setter, timerSetter) {
    var t = setTimeout(function () {
      setter(true);
      setTimeout(function () { setter(false); scheduleBlink(setter, timerSetter); }, CFG.blinkDur);
    }, Math.random() * CFG.blinkRange + CFG.blinkMin);
    timerSetter(t);
  }

  function applyBlink() {
    var eyes = [
      { ids: ['pe1', 'pe2'], blinking: isPurpleBlinking, h: 18 },
      { ids: ['be1', 'be2'], blinking: isBlackBlinking, h: 16 },
      { ids: ['oe1', 'oe2'], blinking: isOrangeBlinking, h: 12 },
      { ids: ['ye1', 'ye2'], blinking: isYellowBlinking, h: 12 }
    ];
    eyes.forEach(function (g) {
      g.ids.forEach(function (id) {
        var el = document.getElementById(id);
        if (!el) return;
        if (g.blinking) {
          el.style.height = '2px';
          el.style.marginTop = (g.h / 2 - 1) + 'px';
        } else {
          el.style.height = g.h + 'px';
          el.style.marginTop = '0px';
        }
      });
    });
  }

  function startAllBlinks() {
    scheduleBlink(function (v) { isPurpleBlinking = v; }, function (t) { purpleBlinkTimer = t; });
    scheduleBlink(function (v) { isBlackBlinking = v; }, function (t) { blackBlinkTimer = t; });
    scheduleBlink(function (v) { isOrangeBlinking = v; }, function (t) { orangeBlinkTimer = t; });
    scheduleBlink(function (v) { isYellowBlinking = v; }, function (t) { yellowBlinkTimer = t; });
  }

  setInterval(applyBlink, 50);

  // ===== Purple peeking =====
  function schedulePeek() {
    if (purplePeekTimer) clearTimeout(purplePeekTimer);
    purplePeekTimer = setTimeout(function () {
      isPurplePeeking = true;
      setTimeout(function () {
        isPurplePeeking = false;
        if (isPasswordVisible && passwordLength > 0) schedulePeek();
      }, 800);
    }, Math.random() * CFG.peekRange + CFG.peekMin);
  }

  // ===== Error state =====
  window._charSetError = function (val) {
    if (val) {
      internalError = true;
      isErrorLocked = true;
      if (errorTimer) clearTimeout(errorTimer);
      errorTimer = setTimeout(function () {
        internalError = false;
        isErrorLocked = false;
      }, CFG.errorRecover);
    }
  };

  // ===== Focus / input detection =====
  document.addEventListener('focusin', function (e) {
    if (e.target.tagName !== 'INPUT') return;
    var prev = focusedField;
    if (e.target.type === 'password') {
      focusedField = 'password';
    } else {
      focusedField = 'username';
    }
    // Brief "look at each other" when switching from password/none to text
    if ((prev === 'password' || prev === 'none') && focusedField === 'username') {
      isLookingAtEachOther = true;
      if (lookingTimer) clearTimeout(lookingTimer);
      lookingTimer = setTimeout(function () { isLookingAtEachOther = false; }, 800);
    }
  });

  document.addEventListener('focusout', function (e) {
    if (e.target.tagName === 'INPUT') {
      setTimeout(function () {
        if (!document.activeElement || document.activeElement.tagName !== 'INPUT') {
          focusedField = 'none';
        }
      }, 100);
    }
  });

  document.addEventListener('input', function (e) {
    if (e.target.tagName !== 'INPUT') return;
    isTyping = true;
    if (e.target.type === 'password') {
      passwordLength = e.target.value.length;
    }
    // Reset error state on input
    if (internalError && !isErrorLocked) {
      internalError = false;
    }
  });

  // ===== Mouse tracking =====
  document.addEventListener('mousemove', function (e) {
    mouseX = e.clientX;
    mouseY = e.clientY;
  });

  // Set default mouse position to center of container
  function setDefaultMouse() {
    if (!container) return;
    var r = container.getBoundingClientRect();
    mouseX = r.left + r.width * 0.5;
    mouseY = r.top + r.height * 0.48;
  }

  // ===== Entrance animation =====
  function doEntrance() {
    var chars = [els.purple, els.black, els.orange, els.yellow];
    chars.forEach(function (el) {
      if (!el) return;
      el.style.transition = 'none';
      el.style.transform = 'translateY(120%) scaleY(0.5)';
      el.style.opacity = '0';
    });

    setTimeout(function () {
      var delays = [0, 120, 60, 180];
      chars.forEach(function (el, i) {
        if (!el) return;
        setTimeout(function () {
          el.style.transition = 'transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.4s ease';
          el.style.transform = 'translateY(0) scaleY(1)';
          el.style.opacity = '1';
          // Restore normal transition after entrance
          setTimeout(function () {
            el.style.transition = 'all ' + CFG.transition + 's ease-in-out';
            entranceDone = true;
          }, 900);
        }, delays[i]);
      });
    }, 100);
  }

  // ===== Init =====
  setDefaultMouse();
  startAllBlinks();
  doEntrance();
  requestAnimationFrame(tick);

})();
