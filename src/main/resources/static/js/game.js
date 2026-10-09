const canvas = document.getElementById('gameCanvas');
 const ctx = canvas.getContext('2d');
// WebSocket & Player ID const playerId = "player_" + Math.floor(Math.random() * 10000); const otherPlayers = {}; let stompClient = null;
// Image များ Load လုပ်ခြင်း const tilesetImg = new Image(); tilesetImg.src = '/Map/hospital.png';
const playerImg = new Image(); playerImg.src = '/Map/jellyb-removebg-preview.png';
// Player Data const player = { x: 0, y: 0, width: 32, height: 32, speed: 4, frameX: 0, frameY: 0, spriteWidth: 64, spriteHeight: 64, moving: false };
// Animation Variables let frameCounter = 0; const frameSpeed = 8; const maxFrames = 4;
// Camera Object const camera = { x: 0, y: 0, width: canvas.width, height: canvas.height };
let mapData = null; let mapWidth = 0;
 let mapHeight = 0; let tileSize = 32;
let bottomLayer = null;
let topLayer = null;
let collisionBoxes = [];
// --- Task / Mini-game Variables --- let taskObjects = []; let nearTask = null; let isTaskOpen = false; let targetR = 180, targetG = 130, targetB = 220;
// WebSocket ချိတ်ဆက်ခြင်း (Task Sync & Disconnect ပါဝင်သော Version) function connectWebSocket() { const socket = new SockJS('/game-websocket'); stompClient = Stomp.over(socket); stompClient.debug = null;
stompClient.connect({}, function (frame) {
    console.log('WebSocket Connected!');

    // 1. Player များ လှုပ်ရှားမှု နှင့် ထွက်သွားမှု စင့်ခ်လုပ်ခြင်း
    stompClient.subscribe('/topic/players', function (msg) { const data = JSON.parse(msg.body);
    // ထွက်သွားကြောင်း မက်ဆေ့ချ် ရောက်လာရင် Canvas ပေါ်မှ ဖျက်ပစ်မည်
    if (data.action === 'LEAVE') {
        delete otherPlayers[data.id];
    } else if (data.id !== playerId) {
        otherPlayers[data.id] = data;
    }
    });

    // 2. Task Completion စင့်ခ်လုပ်ခြင်း (တစ်ယောက်ပြီးရင် အခြားသူများပါ ပျောက်သွားမည်)
    stompClient.subscribe('/topic/task-status', function (msg) {
        const data = JSON.parse(msg.body);

        // Map ပေါ်မှ ပြီးသွားသော Task ကို ဖျက်ပစ်ခြင်း
        taskObjects = taskObjects.filter(task => task.name !== data.taskId);

        // အကယ်၍ ထို Task Window ပွင့်နေပါက ပိတ်လိုက်ခြင်း
        if (nearTask && nearTask.name === data.taskId) {
            closeTaskModal();
            alert(`Player ${data.playerId} has completed ${data.taskId}!`);
        }
    });

});}
function sendPosition() {
if (stompClient && stompClient.connected) {
stompClient.send("/app/move", {}, JSON.stringify({ id: playerId, x: player.x, y: player.y, frameX: player.frameX, frameY: player.frameY })); } }
// Map JSON ဖတ်ယူခြင်း fetch('/Map/hospital.json') .then(response => response.json()) .then(data => { mapData = data; mapWidth = data.width; mapHeight = data.height; tileSize = data.tilewidth;
    data.layers.forEach(layer => {
        if (layer.type === 'tilelayer') {
            if (layer.name === 'Bottom Tile Layer') {
                bottomLayer = layer;
            } else if (layer.name === 'Tile layer') {
                topLayer = layer;
            }
        } else if (layer.type === 'objectgroup') {
            layer.objects.forEach(obj => {
                if (obj.width && obj.height) {
                    if (layer.name === 'Collision' || layer.name === 'Objects') {
                        collisionBoxes.push(obj);
                    }
                }
                if (obj.name === 'PlayerSpawn') {
                    player.x = obj.x;
                    player.y = obj.y;
                }
                if (obj.name && obj.name.toLowerCase().includes('task')) {
                    taskObjects.push(obj);
                }
            });
        }
    });

    connectWebSocket();
    gameLoop();
})
.catch(err => console.error("JSON Map ဖတ်၍ မရပါ:", err));// Keyboard Controls const keys = {}; window.addEventListener('keydown', e => { keys[e.key] = true;
if ((e.key === 'e' || e.key === 'E') && nearTask && !isTaskOpen) {
    openTaskModal();
}
if (e.key === 'Escape' && isTaskOpen) {
    closeTaskModal();
}}); window.addEventListener('keyup', e => delete keys[e.key]);
// Collision Detection function checkCollision(newX, newY) { for (let box of collisionBoxes) { if ( newX < box.x + box.width && newX + player.width > box.x && newY < box.y + box.height && newY + player.height > box.y ) { return true; } } return false; }
function updatePlayer() { player.moving = false; let nextX = player.x; let nextY = player.y;
if (keys['ArrowUp'] || keys['w'] || keys['W'])
 { nextY -= player.speed; player.frameY = 3; player.moving = true; }
if (keys['ArrowDown']  keys['s']
keys['S']) { nextY += player.speed; player.frameY = 0; player.moving = true; }
if (keys['ArrowLeft']  keys['a']
keys['A']) { nextX -= player.speed; player.frameY = 1; player.moving = true; }
if (keys['ArrowRight']  keys['d']
keys['D']) { nextX += player.speed; player.frameY = 2; player.moving = true; }

if (!checkCollision(nextX, player.y)) {
    player.x = nextX;
}if (!checkCollision(player.x, nextY)) {
     player.y = nextY;
 }

 if (player.moving) {
     frameCounter++;
     if (frameCounter % frameSpeed === 0) {
         player.frameX = (player.frameX + 1) % maxFrames;
     }
     sendPosition();
 } else {
     player.frameX = 0;
     frameCounter = 0;
 }}
 function updateCamera() { if (!mapData) return;
 camera.x = (player.x + player.width / 2) - camera.width / 2;
 camera.y = (player.y + player.height / 2) - camera.height / 2;

 const maxCameraX = (mapWidth * tileSize) - camera.width;
 const maxCameraY = (mapHeight * tileSize) - camera.height;

 camera.x = Math.max(0, Math.min(camera.x, maxCameraX));
 camera.y = Math.max(0, Math.min(camera.y, maxCameraY));}
 // --- TASK LOGIC & PROXIMITY --- function checkTaskProximity() { nearTask = null; const interactDistance = 45;
 for (let task of taskObjects) {
     let dx = (player.x + player.width / 2) - (task.x + task.width / 2);
     let dy = (player.y + player.height / 2) - (task.y + task.height / 2);
     let distance = Math.sqrt(dx * dx + dy * dy);

     if (distance < interactDistance) {
         nearTask = task;
         break;
     }
 }}
 function drawTaskPrompt() { if (nearTask && !isTaskOpen) { ctx.fillStyle = "#ffff00"; ctx.font = "bold 16px Arial"; ctx.fillText("Press [E] to start " + nearTask.name, nearTask.x - 25, nearTask.y - 10); } }
 function openTaskModal() { isTaskOpen = true; const content = document.getElementById('taskModalContent');
 // TASK 1: COLOR MATCHING TASK
 if (nearTask.name.toLowerCase() === 'task1') {
     targetR = Math.floor(Math.random() * 200) + 55;
     targetG = Math.floor(Math.random() * 200) + 55;
     targetB = Math.floor(Math.random() * 200) + 55;

     content.innerHTML = `
         <h3 style="color: #00fff5; margin: 0 0 10px 0;">Mix the colors to match the target.</h3>
         <div style="display: flex; justify-content: center; align-items: center; gap: 20px; margin-bottom: 15px;">
             <div>
                 <span style="font-size: 14px; font-weight: bold;">Target:</span><br>
                 <div id="targetCircle" style="width: 70px; height: 70px; border-radius: 50%; background-color: rgb(${targetR}, ${targetG}, ${targetB}); margin: 5px auto; border: 2px solid #fff;"></div>
             </div>
             <div>
                 <span style="font-size: 14px; font-weight: bold;">Your Mix:</span><br>
                 <div id="mixedCircle" style="width: 100px; height: 100px; border-radius: 50%; background-color: rgb(0,0,0); margin: 5px auto; border: 2px solid #fff;"></div>
             </div>
         </div>
         <div style="text-align: left; width: 80%; margin: 0 auto; font-size: 14px;">
             <label>Red</label>
             <input type="range" id="redSlider" min="0" max="255" value="0" oninput="updateColorMix()" style="width: 100%; margin-bottom: 8px;">
             <label>Green</label>
             <input type="range" id="greenSlider" min="0" max="255" value="0" oninput="updateColorMix()" style="width: 100%; margin-bottom: 8px;">
             <label>Blue</label>
             <input type="range" id="blueSlider" min="0" max="255" value="0" oninput="updateColorMix()" style="width: 100%; margin-bottom: 15px;">
         </div>
         <button onclick="checkColorMatch()" style="background: #00fff5; color: #000; font-weight: bold; padding: 10px 25px; border: none; border-radius: 5px; cursor: pointer; width: 80%;">MIX</button>
         <div id="colorStatus" style="margin-top: 10px; font-size: 13px; color: #ff4757;">Not close enough. Keep mixing!</div>
     `;
     setTimeout(updateColorMix, 50);
 }
 // TASK 2: QUIZ
 else if (nearTask.name.toLowerCase() === 'task2') {
     content.innerHTML = `
         <h3 style="color: #00fff5;">Task 2: Medical Quiz</h3>
         <p>အရေးပေါ် ဖုန်းနံပါတ်မှာ မည်သည်နည်း။</p>
         <button onclick="finishTask(false)" style="padding: 8px 15px; margin: 5px;">192</button>
         <button onclick="finishTask(true)" style="padding: 8px 15px; margin: 5px;">199</button>
     `;
 }
 // TASK 3: PASSCODE
 else if (nearTask.name.toLowerCase() === 'task3') {
     content.innerHTML = `<h3 style="color: #00fff5;">Task 3: Security Lock</h3>
                                  <p>Passcode '1234' ကို ရိုက်ပါ:</p>
                                  <input type="password" id="passInput" style="padding: 5px; text-align: center;" maxlength="4"><br><br>
                                  <button onclick="checkTask3Password()" style="padding: 8px 15px;">Submit</button>
                              `;
                          }

                          document.getElementById('taskModal').style.display = 'block';}
                          function updateColorMix() { const r = document.getElementById('redSlider')?.value
                          ra.height;

                          camera.x = Math.max(0, Math.min(camera.x, maxCameraX))0; const b = document.getElementById('blueSlider')?.value || 0;
                          const mixedCircle = document.getElementById('mixedCircle');
                          if (mixedCircle) {
                              mixedCircle.style.backgroundColor = rgb(${r}, ${g}, ${b});
                          }}
                          function checkColorMatch() { const r = parseInt(document.getElementById('redSlider').
                          value);
                          const g = parseInt(document.getElementById('greenSlider').
                          value);
                          const b = parseInt(document.getElementById('blueSlider').
                          value);
                          const diffR = Math.abs(r - targetR);
                          const diffG = Math.abs(g - targetG);
                          const diffB = Math.abs(b - targetB);

                          if (diffR < 40 && diffG < 40 && diffB < 40) {
                              document.getElementById('colorStatus').style.color = '#00ff88';
                              document.getElementById('colorStatus').innerText = "Perfect Match!";
                              setTimeout(() => finishTask(true), 500);
                          } else {
                              document.getElementById('colorStatus').style.color = '#ff4757';
                              document.getElementById('colorStatus').innerText = "Not close enough. Keep mixing!";
                          }
                          }
                          function checkTask3Password() {
                          const val = document.getElementById('passInput').value; if (val === '1234') {
                          finishTask(true); }
                          else
                           {
                          alert("Incorrect Passcode! Try again.");
                          }
                          }
                          function finishTask(success) {
                          if (success) { // 1. WebSocket မှတဆင့် အခြား Player များဆီသို့ Task ပြီးကြောင်း သတင်းပို့ခြင်း if (stompClient && stompClient.connected && nearTask) { stompClient.send("/app/task-complete", {}, JSON.stringify({ taskId: nearTask.name, playerId: playerId, completed: true })); }
                              alert("🎉 Mission Completed: " + nearTask.name);
                              closeTaskModal();
                          } else {
                              alert("❌ Task Failed! Please try again.");
                          }
                          }
                          function closeTaskModal() { isTaskOpen = false;
                          document.getElementById('taskModal').style.display = 'none'; }
                          function drawSingleLayer(layer) {
                           if (!layer || !tilesetImg.complete) return;
                          const data = layer.data;
                          for (let i = 0; i < data.length; i++) {
                              const tileId = data[i];
                              if (tileId > 0) {
                                  const col = i % mapWidth;
                                  const row = Math.floor(i / mapWidth);

                                  const x = col * tileSize;
                                  const y = row * tileSize;

                                  const tilesetCols = Math.floor(tilesetImg.width / tileSize) || 1;
                                  const tileIndex = tileId - 1;
                                  const sx = (tileIndex % tilesetCols) * tileSize;
                                  const sy = Math.floor(tileIndex / tilesetCols) * tileSize;

                                  ctx.drawImage(tilesetImg, sx, sy, tileSize, tileSize, x, y, tileSize, tileSize);
                              }
                          }}
                          function drawPlayer() { if (playerImg.complete) {
                          function drawOtherPlayers() { if (!playerImg.complete) return;
                          for (let id in otherPlayers) {
                              const p = otherPlayers[id];
                              ctx.drawImage(
                                  playerImg,
                                  p.frameX * player.spriteWidth, p.frameY * player.spriteHeight,
                                  player.spriteWidth, player.spriteHeight,
                                  p.x, p.y, player.width, player.height
                              );
                          }}
                          // MAIN GAME LOOP function gameLoop() { ctx.clearRect(0, 0, canvas.width, canvas.height);
                          if (!isTaskOpen) {
                              updatePlayer();
                          }
                          updateCamera();

                          ctx.save();
                          ctx.translate(-camera.x, -camera.y);

                          drawSingleLayer(bottomLayer);
                          drawPlayer();
                          drawOtherPlayers();
                          drawSingleLayer(topLayer);

                          checkTaskProximity();
                          drawTaskPrompt();

                          ctx.restore();

                          requestAnimationFrame(gameLoop);
                          }