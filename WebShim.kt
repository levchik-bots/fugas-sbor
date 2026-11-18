package com.mbteam.fugas

/**
 * JS, который подставляется на страницу Fugas в WebView.
 * Веб-код уже ожидает плагины Capacitor (PushNotifications, App), поэтому даём ему тот же интерфейс,
 * но реализованный нативно. Веб-часть сервера при этом не меняется.
 */
object WebShim {
    const val JS = """
(function(){
  if (window.__fugasShim) return;
  window.__fugasShim = true;

  var listeners = {};
  window.__fugasOnToken = function(t){
    (listeners.registration || []).forEach(function(f){ try { f({value: t}); } catch(e) {} });
  };
  window.__fugasPermResolve = null;

  var PN = {
    checkPermissions: function(){
      return Promise.resolve({receive: FugasNative.hasNotificationPermission() ? 'granted' : 'prompt'});
    },
    requestPermissions: function(){
      return new Promise(function(resolve){
        window.__fugasPermResolve = function(){
          resolve({receive: FugasNative.hasNotificationPermission() ? 'granted' : 'denied'});
        };
        FugasNative.requestNotificationPermission();
      });
    },
    register: function(){ FugasNative.requestToken(); return Promise.resolve(); },
    addListener: function(name, fn){
      (listeners[name] = listeners[name] || []).push(fn);
      return Promise.resolve({remove: function(){}});
    }
  };

  var App = {
    getInfo: function(){ return Promise.resolve({version: FugasNative.appVersion(), name: 'Fugas'}); }
  };

  window.Capacitor = { Plugins: { PushNotifications: PN, App: App } };

  // Кнопка «Установить» обновления: веб качает APK в blob-ссылку и кликает по ней.
  // Перехватываем клик и отдаём установку нативной части (файл уходит во внутреннее хранилище, а не в «Загрузки»).
  document.addEventListener('click', function(e){
    var el = e.target && e.target.closest ? e.target.closest('a[download]') : null;
    if (el && /\.apk$/i.test(el.getAttribute('download') || '')) {
      e.preventDefault();
      e.stopImmediatePropagation();
      FugasNative.installUpdate();
    }
  }, true);
})();
"""
}
