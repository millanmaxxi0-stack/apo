// ===== PeyamApp native bridge (only active inside the Android app, not in a regular browser) =====
// This file is safe to include everywhere: on a normal website it silently does nothing,
// because window.Capacitor only exists inside the native Android shell.
(function () {
  if (!window.Capacitor) return;

  const NativeBridge = window.Capacitor.Plugins && window.Capacitor.Plugins.PeyamPush;
  if (!NativeBridge) { console.log('PeyamPush native plugin not found'); return; }

  let fcmTokenRegistered = false;

  async function registerFcmTokenIfNeeded() {
    if (fcmTokenRegistered || !token) return; // `token` is the app's existing login token (global var in index.html)
    try {
      const result = await NativeBridge.getToken();
      if (result && result.token) {
        await api('/api/fcm-register', { token: result.token }, 'POST');
        fcmTokenRegistered = true;
      }
    } catch (e) { console.log('FCM token registration failed', e); }
  }

  // Call this right after every successful login/auto-login, same places socketAuth() is called.
  window.peyamNativeOnLogin = registerFcmTokenIfNeeded;

  // Handles an action forwarded from a tapped native notification or the native incoming-call screen.
  // `action` payload shape: { type: 'acceptCall'|'declineCall'|'openChat', from, chatId, video }
  async function handleNativeAction(action) {
    if (!action || !action.type) return;
    // Wait until the app has finished logging in and the socket is authenticated.
    const waitForReady = () => new Promise(resolve => {
      const check = () => { if (myUser && socket.connected) resolve(); else setTimeout(check, 200); };
      check();
    });
    await waitForReady();

    if (action.type === 'acceptCall') {
      activeCallWith = action.from;
      isVideoCall = !!action.video;
      document.getElementById('call-name').textContent = action.from;
      document.getElementById('call-status').textContent = 'Connecting...';
      show('call-screen');
      acceptCall();
    } else if (action.type === 'declineCall') {
      activeCallWith = action.from;
      socket.emit('callResponse', { to: action.from, accepted: false });
    } else if (action.type === 'openChat' && action.from) {
      openChat(action.from, action.chatId);
    }
  }

  NativeBridge.addListener('peyamAction', handleNativeAction);

  // If the app was launched fresh (from a killed state) by tapping a notification,
  // ask the native side whether there's a pending action waiting for us.
  NativeBridge.getPendingAction().then(res => { if (res && res.type) handleNativeAction(res); }).catch(() => {});
})();
