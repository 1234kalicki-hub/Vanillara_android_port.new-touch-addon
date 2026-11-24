package com.vanillara.redalert;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import org.libsdl.app.SDLActivity;

/**
 * Hosts the native Red Alert engine. The game data has already been imported
 * into app-private storage by SetupActivity.
 *
 * Added: on-screen ESC / ENTER / SPACE buttons (bottom-left overlay).
 */
public class RedAlertActivity extends SDLActivity {

    @Override
    protected String[] getLibraries() {
        return new String[] {"SDL2", "main"};
    }

    @Override
    protected String getMainFunction() {
        return "SDL_main";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!GameDataStore.prepareForLaunch(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        configureImmersiveMode();
        super.onCreate(savedInstanceState);
        registerBackCallback();
        configureImmersiveMode();
        addKeyButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            configureImmersiveMode();
        }
    }

    // ---- on-screen keys -------------------------------------------------

    private void addKeyButtons() {
        float d = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setFocusable(false);
        row.setFocusableInTouchMode(false);

        row.addView(makeKeyButton("ESC", KeyEvent.KEYCODE_ESCAPE, d));
        row.addView(makeKeyButton("ENTER", KeyEvent.KEYCODE_ENTER, d));
        row.addView(makeKeyButton("SPACE", KeyEvent.KEYCODE_SPACE, d));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.START);
        lp.setMargins((int) (8 * d), 0, 0, (int) (8 * d));
        addContentView(row, lp);
    }

    @SuppressLint("ClickableViewAccessibility")
    private Button makeKeyButton(String label, final int keyCode, float d) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setFocusable(false);
        b.setFocusableInTouchMode(false);
        b.setAlpha(0.6f);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#AA222222"));
        bg.setCornerRadius(8 * d);
        bg.setStroke((int) Math.max(1, d), Color.WHITE);
        b.setBackground(bg);

        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, (int) (6 * d), 0);
        b.setLayoutParams(p);

        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(1.0f);
                        sendKey(KeyEvent.ACTION_DOWN, keyCode);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(0.6f);
                        sendKey(KeyEvent.ACTION_UP, keyCode);
                        return true;
                    default:
                        return true;
                }
            }
        });
        return b;
    }

    private void sendKey(int action, int keyCode) {
        long now = SystemClock.uptimeMillis();
        dispatchKeyEvent(new KeyEvent(now, now, action, keyCode, 0));
    }

    // ---- original code --------------------------------------------------

    private void registerBackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerOnBackInvokedCallback();
        }
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private void registerOnBackInvokedCallback() {
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    sendEscapePulse();
                }
            }
        );
    }

    private void configureImmersiveMode() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        }
    }

    @Override
    public void onBackPressed() {
        sendEscapePulse();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            KeyEvent escapeEvent = new KeyEvent(
                event.getDownTime(),
                event.getEventTime(),
                event.getAction(),
                KeyEvent.KEYCODE_ESCAPE,
                event.getRepeatCount(),
                event.getMetaState(),
                event.getDeviceId(),
                event.getScanCode(),
                event.getFlags(),
                event.getSource()
            );
            super.dispatchKeyEvent(escapeEvent);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void sendEscapePulse() {
        long now = SystemClock.uptimeMillis();
        dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE, 0));
        dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE, 0));
    }
}
