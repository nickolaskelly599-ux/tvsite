package com.exemplo.tvsite

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.view.*
import android.webkit.*
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var cursor: View
    private lateinit var root: FrameLayout
    private var cx = 0f
    private var cy = 0f
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    private val SITE = "https://betterflix.homes"
    private val siteHost = "betterflix.homes"

    private val adHosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "google-analytics.com", "googletagmanager.com",
        "adnxs.com", "taboola.com", "outbrain.com", "popads.net", "popcash.net",
        "propellerads.com", "propellerclick.com", "exoclick.com", "exosrv.com",
        "adsterra.com", "juicyads.com", "clickadu.com", "hilltopads.net",
        "trafficjunky.com", "trafficstars.com", "onclickads.net", "onclkds.com",
        "clickaine.com", "adcash.com", "mgid.com", "revcontent.com", "adsco.re",
        "a-ads.com", "ad-maven.com", "admaven.com", "pushwhy.com", "pushame.com",
        "richpush.co", "evadav.com", "monetag.com", "profitableratecpm.com",
        "highperformanceformat.com", "effectivegatecpm.com", "displayvertising.com",
        "acscdn.com", "yllix.com", "bidgear.com", "tsyndicate.com",
        "realsrv.com", "adskeeper.com", "adspyglass.com", "zeropark.com",
        "popunderjs.com", "bongacams.com", "stripchat.com",
        "shorte.st", "adf.ly", "linkvertise.com"
    )

    private val adKeywords = listOf(
        "/ads/", "/adserver", "popunder", "pop-under", "/banner/ads", "adsbygoogle",
        "/prebid", "clickunder", "/vast.", "/vast?", "popads"
    )

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))

        val d = resources.displayMetrics.density
        val size = (22 * d).toInt()
        cursor = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(200, 255, 255, 255))
                setStroke((2 * d).toInt(), Color.BLACK)
            }
        }
        root.addView(cursor, FrameLayout.LayoutParams(size, size))
        setContentView(root)

        root.post {
            cx = root.width / 2f
            cy = root.height / 2f
            moveCursor()
        }

        web.isFocusable = false
        web.isFocusableInTouchMode = false

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.webViewClient = object : WebViewClient() {

            private fun isAd(host: String, url: String): Boolean {
                if (adHosts.any { host == it || host.endsWith(".$it") }) return true
                val u = url.lowercase()
                return adKeywords.any { u.contains(it) }
            }

            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
                val host = r.url.host ?: return null
                return if (isAd(host, r.url.toString()))
                    WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                else null
            }

            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                if (!r.isForMainFrame) return false
                val host = r.url.host ?: return true
                if (r.url.scheme != "http" && r.url.scheme != "https") return true
                return !(host == siteHost || host.endsWith(".$siteHost"))
            }

            override fun onPageStarted(v: WebView, url: String, f: Bitmap?) {
                injectBlocker(v)
            }

            override fun onPageFinished(v: WebView, url: String) {
                injectBlocker(v)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean = false

            override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                customView = view; customCb = cb
                web.visibility = View.GONE
                cursor.visibility = View.GONE
                root.addView(view, FrameLayout.LayoutParams(-1, -1))
            }

            override fun onHideCustomView() {
                hideCustom()
            }
        }

        web.loadUrl(SITE)
    }

    private fun hideCustom() {
        customView?.let { root.removeView(it) }
        customView = null
        customCb?.onCustomViewHidden()
        customCb = null
        web.visibility = View.VISIBLE
        cursor.visibility = View.VISIBLE
    }

    private fun injectBlocker(v: WebView) {
        v.evaluateJavascript("""
            (function(){
              if (window.__adblock) return; window.__adblock = true;
              window.open = function(){ return null; };

              var s = document.createElement('style');
              s.innerHTML =
                'ins.adsbygoogle,[id^="ad-"],[id^="ads-"],[class^="ad-"],[class^="ads-"],' +
                '[class*="adsbygoogle"],[id*="google_ads"],[class*="banner-ad"],' +
                '[class*="popup-ad"],[id*="popunder"],iframe[src*="ads"],' +
                'iframe[src*="doubleclick"],iframe[src*="popads"]{display:none!important}';
              (document.head || document.documentElement).appendChild(s);

              document.addEventListener('click', function(e){
                var a = e.target.closest && e.target.closest('a[href]');
                if (a && a.target === '_blank' && a.hostname && a.hostname.indexOf('$siteHost') === -1) {
                  e.preventDefault(); e.stopPropagation();
                }
              }, true);

              function clean(){
                var els = document.querySelectorAll('div,a,span');
                for (var i = 0; i < els.length; i++) {
                  var el = els[i], cs = getComputedStyle(el);
                  if ((cs.position === 'fixed' || cs.position === 'absolute') &&
                      el.offsetWidth >= innerWidth * 0.9 && el.offsetHeight >= innerHeight * 0.9 &&
                      parseInt(cs.zIndex || '0') > 1000 &&
                      !el.querySelector('video,iframe') && el.innerText.trim().length < 3) {
                    el.remove();
                  }
                }
              }
              setInterval(clean, 1500);
            })();
        """.trimIndent(), null)
    }

    private fun moveCursor() {
        cursor.translationX = cx
        cursor.translationY = cy
    }

    private fun sendTouch(action: Int) {
        val t = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(t, t, action, cx, cy, 0)
        web.dispatchTouchEvent(ev)
        ev.recycle()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (customView != null) {
            if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) {
                hideCustom()
                return true
            }
            return super.dispatchKeyEvent(e)
        }

        val down = e.action == KeyEvent.ACTION_DOWN
        val d = resources.displayMetrics.density
        val step = (14 + minOf(e.repeatCount * 3, 50)) * d
        val edge = 40 * d

        when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (down) {
                    if (cy <= edge) web.scrollBy(0, -(step * 2).toInt())
                    cy = (cy - step).coerceAtLeast(0f); moveCursor()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (down) {
                    if (cy >= root.height - edge) web.scrollBy(0, (step * 2).toInt())
                    cy = (cy + step).coerceAtMost(root.height - 1f); moveCursor()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (down) { cx = (cx - step).coerceAtLeast(0f); moveCursor() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (down) { cx = (cx + step).coerceAtMost(root.width - 1f); moveCursor() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (e.repeatCount == 0) sendTouch(if (down) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (e.action == KeyEvent.ACTION_UP) {
                    if (web.canGoBack()) web.goBack() else finish()
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (!down) cursor.visibility =
                    if (cursor.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                return true
            }
        }
        return super.dispatchKeyEvent(e)
    }
}
