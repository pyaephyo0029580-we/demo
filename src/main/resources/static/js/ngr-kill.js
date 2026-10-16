/*
 * NGR ROLE + GHOST KILL ANIMATION
 * Backend: Spring Boot + STOMP
 * Character: Cute White Ghost
 */

const NGRKill = (() => {
    "use strict";

    // ====================================
    // 1. GAME STATE
    // ====================================

    const S = {
        client: null,
        roomId: "",
        playerId: "",

        player: null,
        otherPlayers: null,
        playerImg: null,
        portrait: null,
        sendPosition: null,

        joined: false,
        attached: false,
        alive: true,

        role: "UNKNOWN",
        phase: "WAITING",
        hostId: "",

        members: {},
        bodies: [],

        killRange: 90,
        cooldownUntil: 0,
        pending: false,
        animation: null
    };

    const fxCanvas =
        document.getElementById("ngrKillFxCanvas");

    const killBtn =
        document.getElementById("ngrKillBtn");

    const startBtn =
        document.getElementById("ngrStartBtn");

    const roleLabel =
        document.getElementById("ngrRoleLabel");

    const notice =
        document.getElementById("ngrNotice");

    const fx = fxCanvas
        ? fxCanvas.getContext("2d")
        : null;

    let noticeTimer;

    // ====================================
    // 2. SHOW NOTIFICATION
    // ====================================

    function showNotice(message) {
        if (!notice) return;

        notice.textContent = String(message);
        notice.style.display = "block";

        clearTimeout(noticeTimer);

        noticeTimer = setTimeout(() => {
            notice.style.display = "none";
        }, 2300);
    }

    // ====================================
    // 3. SEND WEBSOCKET MESSAGE
    // ====================================

    function send(destination, body = {}) {

        if (!S.client || !S.client.connected) {
            return false;
        }

        S.client.send(
            destination,
            {},
            JSON.stringify(body)
        );

        return true;
    }

    // ====================================
    // 4. ATTACH TO EXISTING NGR GAME
    // ====================================

    function attach(options) {

        if (S.attached) return;

        if (
            !fx ||
            !killBtn ||
            !startBtn ||
            !roleLabel ||
            !notice
        ) {
            console.error(
                "NGRKill: Missing Kill UI elements"
            );
            return;
        }

        if (
            !options ||
            !options.stompClient ||
            !options.stompClient.connected ||
            !options.roomId ||
            !options.playerId ||
            !options.player ||
            !options.otherPlayers ||
            !options.playerImg
        ) {
            console.error(
                "NGRKill: Missing STOMP or player options"
            );
            return;
        }

        S.client = options.stompClient;
        S.roomId = options.roomId;
        S.playerId = options.playerId;
        S.player = options.player;
        S.otherPlayers = options.otherPlayers;
        S.playerImg = options.playerImg;
        S.sendPosition = options.sendPosition || null;
        S.attached = true;

        // Optional: Ghost single PNG image
        if (options.ghostImageUrl) {
            S.portrait = new Image();
            S.portrait.src = options.ghostImageUrl;
        }

        // ====================================
        // PRIVATE ROLE / KILL EVENTS
        // ====================================

        S.client.subscribe(
            "/user/queue/ngr",
            message => {

                const data = JSON.parse(message.body);

                switch (data.type) {

                    case "JOINED":

                        S.joined = true;

                        showNotice("Joined NGR Room");if (
                                                                                  typeof S.sendPosition === "function"
                                                                              ) {
                                                                                  S.sendPosition(true);
                                                                              } else {
                                                                                  send(
                                                                                      "/app/move/" + S.roomId,
                                                                                      {
                                                                                          id: S.playerId,
                                                                                          x: S.player.x,
                                                                                          y: S.player.y,
                                                                                          frameX: S.player.frameX || 0,
                                                                                          frameY: S.player.frameY || 0
                                                                                      }
                                                                                  );
                                                                              }

                                                                              break;

                                                                          case "ROLE":

                                                                              S.role = data.role;

                                                                              S.killRange = Number(
                                                                                  data.killRange || 90
                                                                              );

                                                                              S.alive = true;
                                                                              S.pending = false;
                                                                              S.cooldownUntil = 0;
                                                                              S.animation = null;

                                                                              showNotice(
                                                                                  S.role === "IMPOSTOR"
                                                                                      ? "YOU ARE THE KILLER!"
                                                                                      : "YOU ARE A NORMAL GHOST"
                                                                              );

                                                                              break;

                                                                          case "KILL_EVENT":

                                                                              receiveKill(data);
                                                                              break;

                                                                          case "ERROR":

                                                                              S.pending = false;

                                                                              showNotice(
                                                                                  data.message || "Action denied"
                                                                              );

                                                                              break;
                                                                      }

                                                                      updateUI();
                                                                  }
                                                              );

                                                              // ====================================
                                                              // PUBLIC ROOM STATE
                                                              // ====================================

                                                              S.client.subscribe(
                                                                  "/topic/ngr/" + S.roomId + "/state",
                                                                  message => {

                                                                      const data = JSON.parse(message.body);

                                                                      S.phase = data.phase || "WAITING";
                                                                      S.hostId = data.hostId || "";

                                                                      S.bodies = Array.isArray(data.bodies)
                                                                          ? data.bodies
                                                                          : [];

                                                                      const nextMembers = {};

                                                                      for (const member of data.players || []) {

                                                                          nextMembers[member.id] = member;

                                                                          if (member.id === S.playerId) {

                                                                              S.alive = member.alive !== false;
                                                                              continue;
                                                                          }

                                                                          const old =
                                                                              S.otherPlayers[member.id] || {};

                                                                          S.otherPlayers[member.id] = {
                                                                              ...old,

                                                                              id: member.id,
                                                                              alive: member.alive !== false,
                                                                              positionKnown: member.positionKnown,

                                                                              x: member.positionKnown
                                                                                  ? member.x
                                                                                  : (old.x ?? 0),

                                                                              y: member.positionKnown
                                                                                  ? member.y
                                                                                  : (old.y ?? 0),

                                                                              frameX: old.frameX ?? 0,
                                                                              frameY: old.frameY ?? 0
                                                                          };
                                                                      }

                                                                      // Remove disconnected players
                                                                      for (const id of Object.keys(
                                                                          S.otherPlayers
                                                                      )) {

                                                                          if (!nextMembers[id]) {
                                                                              delete S.otherPlayers[id];
                                                                          }
                                                                      }

                                                                      S.members = nextMembers;

                                                                      updateUI();
                                                                  }
                                                              );

                                                              // ====================================
                                                              // GAME OVER EVENT
                                                              // ====================================

                                                              S.client.subscribe(
                                                                  "/topic/ngr/" + S.roomId + "/game-over",
                                                                  message => {

                                                                      const data = JSON.parse(message.body);

                                                                      S.phase = "FINISHED";showNotice(
                                                                                                               "GAME OVER - " +
                                                                                                               data.winner +
                                                                                                               " WINS"
                                                                                                           );

                                                                                                           updateUI();
                                                                                                       }
                                                                                                   );

                                                                                                   // ====================================
                                                                                                   // BUTTON EVENTS
                                                                                                   // ====================================

                                                                                                   killBtn.addEventListener(
                                                                                                       "click",
                                                                                                       requestKill
                                                                                                   );

                                                                                                   startBtn.addEventListener(
                                                                                                       "click",
                                                                                                       () => {

                                                                                                           if (
                                                                                                               S.hostId === S.playerId &&
                                                                                                               S.phase !== "PLAYING"
                                                                                                           ) {
                                                                                                               send(
                                                                                                                   "/app/ngr/start/" + S.roomId
                                                                                                               );
                                                                                                           }
                                                                                                       }
                                                                                                   );

                                                                                                   // Register player in backend
                                                                                                   send(
                                                                                                       "/app/ngr/join/" + S.roomId,
                                                                                                       {
                                                                                                           playerId: S.playerId
                                                                                                       }
                                                                                                   );
                                                                                               }

                                                                                               // ====================================
                                                                                               // 5. FIND NEAREST KILL TARGET
                                                                                               // ====================================

                                                                                               function nearestTarget() {

                                                                                                   if (
                                                                                                       !S.joined ||
                                                                                                       !S.alive ||
                                                                                                       S.role !== "IMPOSTOR" ||
                                                                                                       S.phase !== "PLAYING" ||
                                                                                                       !S.player
                                                                                                   ) {
                                                                                                       return null;
                                                                                                   }

                                                                                                   let closest = null;
                                                                                                   let best = S.killRange;

                                                                                                   for (const member of Object.values(
                                                                                                       S.members
                                                                                                   )) {

                                                                                                       if (
                                                                                                           member.id === S.playerId ||
                                                                                                           !member.alive ||
                                                                                                           !member.positionKnown
                                                                                                       ) {
                                                                                                           continue;
                                                                                                       }

                                                                                                       const p = S.otherPlayers[member.id];

                                                                                                       if (!p) continue;

                                                                                                       const distance = Math.hypot(
                                                                                                           p.x - S.player.x,
                                                                                                           p.y - S.player.y
                                                                                                       );

                                                                                                       if (distance <= best) {
                                                                                                           best = distance;
                                                                                                           closest = member.id;
                                                                                                       }
                                                                                                   }

                                                                                                   return closest;
                                                                                               }

                                                                                               // ====================================
                                                                                               // 6. KILL BUTTON ACTION
                                                                                               // ====================================

                                                                                               function requestKill() {

                                                                                                   if (
                                                                                                       !S.joined ||
                                                                                                       !S.alive ||
                                                                                                       S.role !== "IMPOSTOR" ||
                                                                                                       S.phase !== "PLAYING" ||
                                                                                                       S.pending ||
                                                                                                       S.animation ||
                                                                                                       performance.now() < S.cooldownUntil
                                                                                                   ) {
                                                                                                       return;
                                                                                                   }

                                                                                                   const targetId = nearestTarget();

                                                                                                   if (!targetId) {
                                                                                                       showNotice(
                                                                                                           "Move closer to another ghost"
                                                                                                       );
                                                                                                       return;
                                                                                                   }

                                                                                                   S.pending = true;

                                                                                                   const sent = send(
                                                                                                       "/app/ngr/kill/" + S.roomId,
                                                                                                       {
                                                                                                           targetId: targetId
                                                                                                       }
                                                                                                   );

                                                                                                   if (!sent) {
                                                                                                       S.pending = false;
                                                                                                   }
                                                                                               }

                                                                                               // ====================================
                                                                                               // 7. RECEIVE KILL EVENT
                                                                                               // ====================================

                                                                                               function receiveKill(data) {

                                                                                                   S.pending = false;

                                                                                                   // Killer cooldown
                                                                                                   if (data.killerId === S.playerId) {

                                                                                                       S.cooldownUntil =
                                                                                                           performance.now() +
                                                                                                           Number(data.cooldownMs || 10000);
                                                                                                   }

                                                                                                   // Victim is dead
                                                                                                   if (data.targetId === S.playerId) {
                                                                                                       S.alive = false;
                                                                                                   }

                                                                                                   if (S.otherPlayers[data.targetId]) {
                                                                                                       S.otherPlayers[data.targetId].alive = false;
                                                                                                   }

                                                                                                   // Start ghost animation
                                                                                                   S.animation = {
                                                                                                       ...data,
                                                                                                       start: performance.now()
                                                                                                   };
                                                                                               }

                                                                                               // ====================================
                                                                                               // 8. KILL BUTTON / ROLE UI
                                                                                               // ====================================

                                                                                               function updateUI() {

                                                                                                   if (
                                                                                                       !killBtn ||
                                                                                                       !startBtn ||
                                                                                                       !roleLabel
                                                                                                   ) {
                                                                                                       return;
                                                                                                   }

                                                                                                   // Host start button
                                                                                                   startBtn.style.display =
                                                                                                       S.joined &&
                                                                                                       S.hostId === S.playerId &&
                                                                                                       S.phase !== "PLAYING"
                                                                                                           ? "block"
                                                                                                           : "none";// Only killer sees HAUNT button
                                                                                                                            killBtn.style.display =
                                                                                                                                S.joined &&
                                                                                                                                S.role === "IMPOSTOR" &&
                                                                                                                                S.phase === "PLAYING" &&
                                                                                                                                S.alive
                                                                                                                                    ? "block"
                                                                                                                                    : "none";

                                                                                                                            const remaining = Math.max(
                                                                                                                                0,
                                                                                                                                Math.ceil(
                                                                                                                                    (S.cooldownUntil -
                                                                                                                                        performance.now()) / 1000
                                                                                                                                )
                                                                                                                            );

                                                                                                                            killBtn.textContent =
                                                                                                                                remaining > 0
                                                                                                                                    ? remaining + "s"
                                                                                                                                    : "HAUNT";

                                                                                                                            killBtn.disabled =
                                                                                                                                remaining > 0 ||
                                                                                                                                S.pending ||
                                                                                                                                !!S.animation ||
                                                                                                                                !nearestTarget();

                                                                                                                            const displayRole =
                                                                                                                                S.role === "IMPOSTOR"
                                                                                                                                    ? "KILLER"
                                                                                                                                    : S.role === "CREW"
                                                                                                                                        ? "NORMAL"
                                                                                                                                        : "UNKNOWN";

                                                                                                                            roleLabel.textContent =
                                                                                                                                "Role: " + displayRole +
                                                                                                                                " | " + S.phase +
                                                                                                                                (S.alive ? "" : " | DEAD");
                                                                                                                        }

                                                                                                                        // ====================================
                                                                                                                        // 9. DRAW GHOST CHARACTER
                                                                                                                        // ====================================

                                                                                                                        function drawGhost(
                                                                                                                            g,
                                                                                                                            x,
                                                                                                                            y,
                                                                                                                            size,
                                                                                                                            opts = {}
                                                                                                                        ) {

                                                                                                                            const {
                                                                                                                                alpha = 1,
                                                                                                                                angle = 0,
                                                                                                                                scaleX = 1,
                                                                                                                                scaleY = 1,
                                                                                                                                glow = 0
                                                                                                                            } = opts;

                                                                                                                            g.save();

                                                                                                                            g.translate(x, y);
                                                                                                                            g.rotate(angle);
                                                                                                                            g.scale(scaleX, scaleY);

                                                                                                                            g.globalAlpha *= alpha;

                                                                                                                            // Soft blue aura
                                                                                                                            if (glow > 0) {

                                                                                                                                const gradient =
                                                                                                                                    g.createRadialGradient(
                                                                                                                                        0, 0, size * 0.15,
                                                                                                                                        0, 0, size * 0.75
                                                                                                                                    );

                                                                                                                                gradient.addColorStop(
                                                                                                                                    0,
                                                                                                                                    "rgba(190,236,255,0.52)"
                                                                                                                                );

                                                                                                                                gradient.addColorStop(
                                                                                                                                    1,
                                                                                                                                    "rgba(190,236,255,0)"
                                                                                                                                );

                                                                                                                                g.fillStyle = gradient;

                                                                                                                                g.beginPath();

                                                                                                                                g.arc(
                                                                                                                                    0,
                                                                                                                                    0,
                                                                                                                                    size * 0.75,
                                                                                                                                    0,
                                                                                                                                    Math.PI * 2
                                                                                                                                );

                                                                                                                                g.fill();
                                                                                                                            }

                                                                                                                            const portrait = S.portrait;
                                                                                                                            const sprite = S.playerImg;

                                                                                                                            const sw =
                                                                                                                                S.player?.spriteWidth || 64;

                                                                                                                            const sh =
                                                                                                                                S.player?.spriteHeight || 64;

                                                                                                                            // Single Ghost PNG
                                                                                                                            if (
                                                                                                                                portrait &&
                                                                                                                                portrait.complete &&
                                                                                                                                portrait.naturalWidth
                                                                                                                            ) {

                                                                                                                                g.drawImage(
                                                                                                                                    portrait,
                                                                                                                                    -size / 2,
                                                                                                                                    -size / 2,
                                                                                                                                    size,
                                                                                                                                    size
                                                                                                                                );

                                                                                                                            // Existing NGR sprite sheet
                                                                                                                            } else if (
                                                                                                                                sprite &&
                                                                                                                                sprite.complete &&
                                                                                                                                sprite.naturalWidth >= sw &&
                                                                                                                                sprite.naturalHeight >= sh
                                                                                                                            ) {

                                                                                                                                g.drawImage(
                                                                                                                                    sprite,
                                                                                                                                    0, 0,
                                                                                                                                    sw, sh,
                                                                                                                                    -size / 2,
                                                                                                                                    -size / 2,
                                                                                                                                    size, size
                                                                                                                                );

                                                                                                                            } else {

                                                                                                                                // Fallback cute ghost
                                                                                                                                g.fillStyle = "#f5fbff";
                                                                                                                                g.strokeStyle = "#28313d";

                                                                                                                                g.lineWidth = Math.max(
                                                                                                                                    2,
                                                                                                                                    size * 0.025
                                                                                                                                );

                                                                                                                                g.beginPath();

                                                                                                                                g.moveTo(
                                                                                                                                    -size * 0.30,
                                                                                                                                    size * 0.30
                                                                                                                                );

                                                                                                                                g.lineTo(
                                                                                                                                    -size * 0.30,
                                                                                                                                    -size * 0.05
                                                                                                                                );

                                                                                                                                g.bezierCurveTo(
                                                                                                                                    -size * 0.30,
                                                                                                                                    -size * 0.46,
                                                                                                                                    size * 0.30,
                                                                                                                                    -size * 0.46,
                                                                                                                                    size * 0.30,
                                                                                                                                    -size * 0.05
                                                                                                                                );

                                                                                                                                g.lineTo(
                                                                                                                                    size * 0.30,
                                                                                                                                    size * 0.30
                                                                                                                                );

                                                                                                                                g.quadraticCurveTo(
                                                                                                                                    size * 0.20,
                                                                                                                                    size * 0.43,
                                                                                                                                    size * 0.10,
                                                                                                                                    size * 0.30
                                                                                                                                );g.quadraticCurveTo(
                                                                                                                                                  0,
                                                                                                                                                  size * 0.43,
                                                                                                                                                  -size * 0.11,
                                                                                                                                                  size * 0.30
                                                                                                                                              );

                                                                                                                                              g.quadraticCurveTo(
                                                                                                                                                  -size * 0.23,
                                                                                                                                                  size * 0.43,
                                                                                                                                                  -size * 0.30,
                                                                                                                                                  size * 0.30
                                                                                                                                              );

                                                                                                                                              g.fill();
                                                                                                                                              g.stroke();

                                                                                                                                              // Eyes
                                                                                                                                              g.fillStyle = "#19242c";

                                                                                                                                              for (const eyeX of [-0.11, 0.10]) {

                                                                                                                                                  g.beginPath();

                                                                                                                                                  g.ellipse(
                                                                                                                                                      size * eyeX,
                                                                                                                                                      0,
                                                                                                                                                      size * 0.03,
                                                                                                                                                      size * 0.07,
                                                                                                                                                      0,
                                                                                                                                                      0,
                                                                                                                                                      Math.PI * 2
                                                                                                                                                  );

                                                                                                                                                  g.fill();
                                                                                                                                              }

                                                                                                                                              // Cute pink cheeks
                                                                                                                                              g.fillStyle = "#ffc2d5";

                                                                                                                                              for (const cheekX of [-0.20, 0.20]) {

                                                                                                                                                  g.beginPath();

                                                                                                                                                  g.arc(
                                                                                                                                                      size * cheekX,
                                                                                                                                                      size * 0.14,
                                                                                                                                                      size * 0.05,
                                                                                                                                                      0,
                                                                                                                                                      Math.PI * 2
                                                                                                                                                  );

                                                                                                                                                  g.fill();
                                                                                                                                              }
                                                                                                                                          }

                                                                                                                                          g.restore();
                                                                                                                                      }

                                                                                                                                      // ====================================
                                                                                                                                      // 10. DEAD GHOST BODY
                                                                                                                                      // ====================================

                                                                                                                                      function drawBodies(g) {

                                                                                                                                          if (!g) return;

                                                                                                                                          for (const body of S.bodies) {

                                                                                                                                              const x = Number(body.x) + 16;
                                                                                                                                              const y = Number(body.y) + 19;

                                                                                                                                              if (
                                                                                                                                                  !Number.isFinite(x) ||
                                                                                                                                                  !Number.isFinite(y)
                                                                                                                                              ) {
                                                                                                                                                  continue;
                                                                                                                                              }

                                                                                                                                              g.save();

                                                                                                                                              // Light blue ghost ring
                                                                                                                                              g.strokeStyle =
                                                                                                                                                  "rgba(160,219,255,0.78)";

                                                                                                                                              g.lineWidth = 1.5;

                                                                                                                                              g.beginPath();

                                                                                                                                              g.ellipse(
                                                                                                                                                  x,
                                                                                                                                                  y + 5,
                                                                                                                                                  18,
                                                                                                                                                  7,
                                                                                                                                                  0,
                                                                                                                                                  0,
                                                                                                                                                  Math.PI * 2
                                                                                                                                              );

                                                                                                                                              g.stroke();

                                                                                                                                              // Flattened ghost body
                                                                                                                                              drawGhost(
                                                                                                                                                  g,
                                                                                                                                                  x,
                                                                                                                                                  y,
                                                                                                                                                  31,
                                                                                                                                                  {
                                                                                                                                                      alpha: 0.65,
                                                                                                                                                      angle: -0.25,
                                                                                                                                                      scaleY: 0.48
                                                                                                                                                  }
                                                                                                                                              );

                                                                                                                                              g.restore();
                                                                                                                                          }
                                                                                                                                      }

                                                                                                                                      // ====================================
                                                                                                                                      // 11. ANIMATION HELPERS
                                                                                                                                      // ====================================

                                                                                                                                      const clamp01 = x =>
                                                                                                                                          Math.max(0, Math.min(1, x));

                                                                                                                                      const easeOut = t =>
                                                                                                                                          1 - Math.pow(
                                                                                                                                              1 - clamp01(t),
                                                                                                                                              3
                                                                                                                                          );

                                                                                                                                      // ====================================
                                                                                                                                      // 12. GHOST KILL ANIMATION
                                                                                                                                      // ====================================

                                                                                                                                      function renderFx(now = performance.now()) {

                                                                                                                                          if (!fx) return;

                                                                                                                                          fx.clearRect(
                                                                                                                                              0,
                                                                                                                                              0,
                                                                                                                                              fxCanvas.width,
                                                                                                                                              fxCanvas.height
                                                                                                                                          );

                                                                                                                                          updateUI();

                                                                                                                                          const animation = S.animation;

                                                                                                                                          if (!animation) return;

                                                                                                                                          const duration =
                                                                                                                                              Number(animation.durationMs) || 1400;

                                                                                                                                          const elapsed =
                                                                                                                                              now - animation.start;

                                                                                                                                          const progress =
                                                                                                                                              clamp01(elapsed / duration);

                                                                                                                                          if (progress >= 1) {

                                                                                                                                              S.animation = null;
                                                                                                                                              return;
                                                                                                                                          }

                                                                                                                                          fx.save();

                                                                                                                                          fx.scale(
                                                                                                                                              fxCanvas.width / 800,
                                                                                                                                              fxCanvas.height / 600
                                                                                                                                          );

                                                                                                                                          // Fade in / fade out
                                                                                                                                          const fade = Math.min(
                                                                                                                                              1,
                                                                                                                                              progress * 14,
                                                                                                                                              (1 - progress) * 11
                                                                                                                                          );

                                                                                                                                          fx.globalAlpha =
                                                                                                                                              Math.max(0, fade);

                                                                                                                                          // ====================================
                                                                                                                                          // SCENE 1: GHOST BACKGROUND
                                                                                                                                          // ====================================

                                                                                                                                          fx.fillStyle =
                                                                                                                                              "rgba(14,23,41,0.90)";

                                                                                                                                          fx.fillRect(
                                                                                                                                              0, 0,
                                                                                                                                              800, 600
                                                                                                                                          );// Soft blue stage
                                                                                                                                                    const band =
                                                                                                                                                        fx.createLinearGradient(
                                                                                                                                                            0, 150,
                                                                                                                                                            0, 440
                                                                                                                                                        );

                                                                                                                                                    band.addColorStop(
                                                                                                                                                        0,
                                                                                                                                                        "#5f8ba8"
                                                                                                                                                    );

                                                                                                                                                    band.addColorStop(
                                                                                                                                                        0.5,
                                                                                                                                                        "#c5e9f7"
                                                                                                                                                    );

                                                                                                                                                    band.addColorStop(
                                                                                                                                                        1,
                                                                                                                                                        "#5f8ba8"
                                                                                                                                                    );

                                                                                                                                                    fx.fillStyle = band;

                                                                                                                                                    fx.fillRect(
                                                                                                                                                        0,
                                                                                                                                                        160,
                                                                                                                                                        800,
                                                                                                                                                        280
                                                                                                                                                    );

                                                                                                                                                    // ====================================
                                                                                                                                                    // SCENE 2: FLOATING ENERGY LINES
                                                                                                                                                    // ====================================

                                                                                                                                                    for (let i = 0; i < 11; i++) {

                                                                                                                                                        const x =
                                                                                                                                                            (now *
                                                                                                                                                                (0.07 + i * 0.007) +
                                                                                                                                                                i * 93) % 1100 - 150;

                                                                                                                                                        fx.fillStyle =
                                                                                                                                                            i % 2
                                                                                                                                                                ? "rgba(255,255,255,0.25)"
                                                                                                                                                                : "rgba(143,209,241,0.45)";

                                                                                                                                                        fx.fillRect(
                                                                                                                                                            x,
                                                                                                                                                            184 + i * 22,
                                                                                                                                                            95 + i * 8,
                                                                                                                                                            2 + i % 3
                                                                                                                                                        );
                                                                                                                                                    }

                                                                                                                                                    // ====================================
                                                                                                                                                    // SCENE 3: KILLER GHOST DASH
                                                                                                                                                    // ====================================

                                                                                                                                                    const dash =
                                                                                                                                                        easeOut(
                                                                                                                                                            (progress - 0.15) / 0.29
                                                                                                                                                        ) * 145;

                                                                                                                                                    const killerX =
                                                                                                                                                        238 + dash;

                                                                                                                                                    const killerY =
                                                                                                                                                        297 + Math.sin(progress * 18) * 4;

                                                                                                                                                    const stretch =
                                                                                                                                                        progress > 0.16 &&
                                                                                                                                                        progress < 0.47
                                                                                                                                                            ? 1.12
                                                                                                                                                            : 1;

                                                                                                                                                    drawGhost(
                                                                                                                                                        fx,
                                                                                                                                                        killerX,
                                                                                                                                                        killerY,
                                                                                                                                                        126,
                                                                                                                                                        {
                                                                                                                                                            glow: 1,
                                                                                                                                                            scaleX: stretch,
                                                                                                                                                            scaleY: 1 / stretch
                                                                                                                                                        }
                                                                                                                                                    );

                                                                                                                                                    // ====================================
                                                                                                                                                    // SCENE 4: VICTIM GHOST SHAKE
                                                                                                                                                    // ====================================

                                                                                                                                                    if (progress < 0.53) {

                                                                                                                                                        const shake =
                                                                                                                                                            progress > 0.36
                                                                                                                                                                ? Math.sin(now * 0.13) * 9
                                                                                                                                                                : 0;

                                                                                                                                                        drawGhost(
                                                                                                                                                            fx,
                                                                                                                                                            543 + shake,
                                                                                                                                                            298,
                                                                                                                                                            120,
                                                                                                                                                            {
                                                                                                                                                                glow: 0.15
                                                                                                                                                            }
                                                                                                                                                        );

                                                                                                                                                    } else {

                                                                                                                                                        // ====================================
                                                                                                                                                        // SCENE 5: VICTIM FALLS DOWN
                                                                                                                                                        // ====================================

                                                                                                                                                        const fall = easeOut(
                                                                                                                                                            (progress - 0.53) / 0.28
                                                                                                                                                        );

                                                                                                                                                        drawGhost(
                                                                                                                                                            fx,
                                                                                                                                                            543,
                                                                                                                                                            298 + fall * 64,
                                                                                                                                                            120,
                                                                                                                                                            {
                                                                                                                                                                angle: fall * 0.45,
                                                                                                                                                                scaleX: 1 + fall * 0.24,
                                                                                                                                                                scaleY: 1 - fall * 0.65,
                                                                                                                                                                alpha: 1 - fall * 0.70
                                                                                                                                                            }
                                                                                                                                                        );
                                                                                                                                                    }

                                                                                                                                                    // ====================================
                                                                                                                                                    // SCENE 6: GHOST ENERGY ATTACK
                                                                                                                                                    // ====================================

                                                                                                                                                    if (
                                                                                                                                                        progress > 0.32 &&
                                                                                                                                                        progress < 0.64
                                                                                                                                                    ) {

                                                                                                                                                        const spread = easeOut(
                                                                                                                                                            (progress - 0.32) / 0.15
                                                                                                                                                        );

                                                                                                                                                        const tipX =
                                                                                                                                                            380 + 180 * spread;

                                                                                                                                                        const dissipate = clamp01(
                                                                                                                                                            (0.64 - progress) / 0.16
                                                                                                                                                        );

                                                                                                                                                        fx.save();

                                                                                                                                                        fx.globalAlpha *= dissipate;

                                                                                                                                                        fx.lineCap = "round";

                                                                                                                                                        // Blue ghost energy
                                                                                                                                                        fx.strokeStyle =
                                                                                                                                                            "rgba(84,177,227,0.95)";

                                                                                                                                                        fx.lineWidth = 30;

                                                                                                                                                        fx.beginPath();

                                                                                                                                                        fx.moveTo(
                                                                                                                                                            379,
                                                                                                                                                            310
                                                                                                                                                        );

                                                                                                                                                        fx.quadraticCurveTo(
                                                                                                                                                            460,
                                                                                                                                                            237,
                                                                                                                                                            tipX,
                                                                                                                                                            293
                                                                                                                                                        );

                                                                                                                                                        fx.stroke();

                                                                                                                                                        // White glowing core
                                                                                                                                                        fx.strokeStyle =
                                                                                                                                                            "rgba(255,255,255,0.98)";

                                                                                                                                                        fx.lineWidth = 9;

                                                                                                                                                        fx.stroke();fx.restore();
                                                                                                                                                                            }

                                                                                                                                                                            // ====================================
                                                                                                                                                                            // SCENE 7: GHOST HIT SPARKLES
                                                                                                                                                                            // ====================================

                                                                                                                                                                            if (
                                                                                                                                                                                progress > 0.40 &&
                                                                                                                                                                                progress < 0.70
                                                                                                                                                                            ) {

                                                                                                                                                                                const t = clamp01(
                                                                                                                                                                                    (progress - 0.40) / 0.30
                                                                                                                                                                                );

                                                                                                                                                                                fx.save();

                                                                                                                                                                                fx.globalAlpha *= (1 - t);

                                                                                                                                                                                fx.strokeStyle = "#ffffff";
                                                                                                                                                                                fx.lineWidth = 4;

                                                                                                                                                                                fx.beginPath();

                                                                                                                                                                                fx.arc(
                                                                                                                                                                                    540,
                                                                                                                                                                                    295,
                                                                                                                                                                                    18 + t * 70,
                                                                                                                                                                                    0,
                                                                                                                                                                                    Math.PI * 2
                                                                                                                                                                                );

                                                                                                                                                                                fx.stroke();

                                                                                                                                                                                // Sparkle particles
                                                                                                                                                                                for (let i = 0; i < 9; i++) {

                                                                                                                                                                                    const angle =
                                                                                                                                                                                        i * Math.PI * 2 / 9;

                                                                                                                                                                                    const radius =
                                                                                                                                                                                        25 + t * 85;

                                                                                                                                                                                    fx.fillStyle =
                                                                                                                                                                                        i % 2
                                                                                                                                                                                            ? "#d7f4ff"
                                                                                                                                                                                            : "#ffffff";

                                                                                                                                                                                    fx.beginPath();

                                                                                                                                                                                    fx.arc(
                                                                                                                                                                                        540 + Math.cos(angle) * radius,
                                                                                                                                                                                        295 + Math.sin(angle) * radius,
                                                                                                                                                                                        4,
                                                                                                                                                                                        0,
                                                                                                                                                                                        Math.PI * 2
                                                                                                                                                                                    );

                                                                                                                                                                                    fx.fill();
                                                                                                                                                                                }

                                                                                                                                                                                fx.restore();
                                                                                                                                                                            }

                                                                                                                                                                            // ====================================
                                                                                                                                                                            // SCENE 8: VICTIM SOUL FLOATS UP
                                                                                                                                                                            // ====================================

                                                                                                                                                                            if (
                                                                                                                                                                                progress > 0.58 &&
                                                                                                                                                                                progress < 0.92
                                                                                                                                                                            ) {

                                                                                                                                                                                const soulProgress = clamp01(
                                                                                                                                                                                    (progress - 0.58) / 0.34
                                                                                                                                                                                );

                                                                                                                                                                                drawGhost(
                                                                                                                                                                                    fx,

                                                                                                                                                                                    557 +
                                                                                                                                                                                    Math.sin(soulProgress * 7) * 10,

                                                                                                                                                                                    270 - soulProgress * 120,

                                                                                                                                                                                    36,

                                                                                                                                                                                    {
                                                                                                                                                                                        alpha:
                                                                                                                                                                                            (1 - soulProgress) * 0.8,

                                                                                                                                                                                        glow: 1
                                                                                                                                                                                    }
                                                                                                                                                                                );
                                                                                                                                                                            }

                                                                                                                                                                            // ====================================
                                                                                                                                                                            // SCENE 9: GHOST ATTACK TEXT
                                                                                                                                                                            // ====================================

                                                                                                                                                                            fx.textAlign = "center";
                                                                                                                                                                            fx.font = "bold 36px Arial";

                                                                                                                                                                            fx.lineWidth = 5;
                                                                                                                                                                            fx.strokeStyle = "#28455e";

                                                                                                                                                                            fx.strokeText(
                                                                                                                                                                                "GHOST ATTACK!",
                                                                                                                                                                                400,
                                                                                                                                                                                110
                                                                                                                                                                            );

                                                                                                                                                                            fx.fillStyle = "#ffffff";

                                                                                                                                                                            fx.fillText(
                                                                                                                                                                                "GHOST ATTACK!",
                                                                                                                                                                                400,
                                                                                                                                                                                110
                                                                                                                                                                            );

                                                                                                                                                                            fx.restore();
                                                                                                                                                                        }

                                                                                                                                                                        // ====================================
                                                                                                                                                                        // 13. FUNCTIONS FOR player.html
                                                                                                                                                                        // ====================================

                                                                                                                                                                        return {

                                                                                                                                                                            attach: attach,

                                                                                                                                                                            requestKill: requestKill,

                                                                                                                                                                            drawBodies: drawBodies,

                                                                                                                                                                            renderFx: renderFx,

                                                                                                                                                                            isAlive: () => S.alive,

                                                                                                                                                                            isDead: id =>
                                                                                                                                                                                S.members[id]?.alive === false,

                                                                                                                                                                            canMove: () =>
                                                                                                                                                                                S.joined &&
                                                                                                                                                                                S.alive &&
                                                                                                                                                                                S.phase !== "FINISHED" &&
                                                                                                                                                                                !S.animation,

                                                                                                                                                                            canDoTasks: () =>
                                                                                                                                                                                S.joined &&
                                                                                                                                                                                S.alive &&
                                                                                                                                                                                S.phase === "PLAYING" &&
                                                                                                                                                                                S.role === "CREW" &&
                                                                                                                                                                                !S.animation
                                                                                                                                                                        };

                                                                                                                                                                    })();