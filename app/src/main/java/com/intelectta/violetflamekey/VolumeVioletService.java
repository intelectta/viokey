package com.intelectta.violetflamekey;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class VolumeVioletService extends AccessibilityService {
    private static final String PHRASE = "violetflame";
    private static final long LONG_PRESS_MS = 650L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean volumeUpHeld = false;
    private boolean alreadyTriggered = false;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (volumeUpHeld && !alreadyTriggered) {
                alreadyTriggered = true;
                writePhrase();
                vibrateShort();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_VOLUME_UP) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!volumeUpHeld) {
                volumeUpHeld = true;
                alreadyTriggered = false;
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_UP) {
            volumeUpHeld = false;
            handler.removeCallbacks(longPressRunnable);
            return alreadyTriggered;
        }

        return false;
    }

    private void writePhrase() {
        AccessibilityNodeInfo focused = findFocusedEditableNode();
        if (focused == null) return;

        CharSequence current = focused.getText();
        String newText = (current == null ? "" : current.toString()) + PHRASE;

        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
        boolean ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

        if (!ok) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("violetflame", PHRASE));
                focused.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
        }
        focused.recycle();
    }

    private AccessibilityNodeInfo findFocusedEditableNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo node = findFocusedEditableNodeRecursive(root);
        root.recycle();
        return node;
    }

    private AccessibilityNodeInfo findFocusedEditableNodeRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isFocused() && node.isEditable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findFocusedEditableNodeRecursive(child);
            if (child != null) child.recycle();
            if (result != null) return result;
        }
        return null;
    }

    private void vibrateShort() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(45);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }
}
