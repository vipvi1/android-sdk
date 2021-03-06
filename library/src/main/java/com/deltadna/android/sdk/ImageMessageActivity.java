/*
 * Copyright (c) 2016 deltaDNA Ltd. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.deltadna.android.sdk.listeners.EngageListener;
import com.deltadna.android.sdk.listeners.ImageMessageResultListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * {@link Activity} which displays in Image Message request and handles the
 * user interaction with the Image Message.
 *
 * @see DDNA#requestEngagement(Engagement, EngageListener)
 */
public final class ImageMessageActivity extends Activity {
    
    private static final String TAG = BuildConfig.LOG_TAG +
            ' ' +
            ImageMessageActivity.class.getSimpleName();
    
    private static final String EXTRA_IMG_MSG = "img_msg";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_PARAMS = "params";
    
    private ImageMessage imageMessage;
    
    private Bitmap bitmap;
    
    int screenWidth = 0;
    int screenHeight = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        imageMessage = (ImageMessage) getIntent().getSerializableExtra(
                EXTRA_IMG_MSG);
        if (!imageMessage.prepared()) {
            throw new IllegalStateException(
                    "Image Message must be prepared first");
        }
        
        final RelativeLayout layout = new RelativeLayout(this);
        
        final FrameLayout holder = new FrameLayout(this);
        layout.addView(holder, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        
        final ImageMessageView viewImageMessage = new ImageMessageView(this);
        holder.addView(viewImageMessage, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        
        final RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        
        setContentView(layout);
    }
    
    /**
     * Handles an action defined by the Delta DNA message.
     *
     * @param source    the source of the action
     * @param action    the action to handle
     */
    private void performAction(
            String source,
            @Nullable ImageMessage.BaseAction action){
        
        if (action != null) {
            final Event event = createActionEvent(source, action);
            
            if (action instanceof ImageMessage.DismissAction) {
                setResult(Activity.RESULT_CANCELED);
            } else {
                setResult(Activity.RESULT_OK, new Intent()
                        .putExtra(EXTRA_ACTION, action)
                        .putExtra(EXTRA_PARAMS, imageMessage.parameters().toString()));
                
                event.putParam("imActionValue", action.getValue());
            }
            
            DDNA.instance().recordEvent(event).run();
            
            finish();
        }
    }
    
    private Event createActionEvent(String source, ImageMessage.BaseAction action) {
        JSONObject params;
        try {
            params = new JSONObject(imageMessage.eventParams);
        } catch (JSONException e) {
            Log.w(  BuildConfig.LOG_TAG,
                    "Failed to convert eventParams to JSON",
                    e);
            params = new JSONObject();
        }
        
        return new Event("imageMessageAction")
                .putParam(
                        "responseTransactionID",
                        params.optLong("responseTransactionID", -1))
                .putParam(
                        "responseDecisionpointName",
                        params.optString("responseDecisionpointName"))
                .putParam(
                        "responseEngagementID",
                        params.optLong("responseEngagementID", -1))
                .putParam(
                        "responseEngagementName",
                        params.optString("responseEngagementName"))
                .putParam(
                        "responseEngagementType",
                        params.optString("responseEngagementType"))
                .putParam(
                        "responseMessageSequence",
                        params.optLong("responseMessageSequence", -1))
                .putParam(
                        "responseVariantName",
                        params.optString("responseVariantName"))
                .putParam(
                        "imActionName",
                        source)
                .putParam(
                        "imActionType",
                        action.type);
    }
    
    public static Intent createIntent(Context context, ImageMessage msg) {
        return new Intent(context, ImageMessageActivity.class)
                .putExtra(EXTRA_IMG_MSG, msg);
    }
    
    public static void handleResult(
            int resultCode,
            Intent data,
            ImageMessageResultListener callback) {
        
        if (resultCode == RESULT_OK) {
            final ImageMessage.BaseAction action = (ImageMessage.BaseAction)
                    data.getSerializableExtra(EXTRA_ACTION);
            JSONObject params;
            try {
                params = new JSONObject(data.getStringExtra(EXTRA_PARAMS));
            } catch (JSONException e) {
                Log.w(TAG, "Failed deserialising params to JSON", e);
                params = new JSONObject();
            }
            
            if (action instanceof ImageMessage.LinkAction) {
                callback.onLink(
                        ((ImageMessage.LinkAction) action).getValue(),
                        params);
            } else if (action instanceof ImageMessage.Action) {
                callback.onAction(
                        ((ImageMessage.Action) action).getValue(),
                        params);
            } else if (action instanceof ImageMessage.StoreAction) {
                callback.onStore(
                        ((ImageMessage.StoreAction) action).getValue(),
                        params);
            } else {
                Log.w(TAG, "Unknown action type: " + action);
            }
        } else if (resultCode == RESULT_CANCELED) {
            callback.onCancelled();
        }
    }
    
    private class ImageMessageView extends View implements
            View.OnTouchListener {
        
        public ImageMessageView(Context context) {
            super(context);
            setOnTouchListener(this);
        }
        
        public ImageMessageView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnTouchListener(this);
        }
        
        @Override
        protected void onLayout(
                boolean changed,
                int left,
                int top,
                int right,
                int bottom) {
            
            screenWidth = right;
            screenHeight = bottom;
            
            if (bitmap == null) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeFile(
                        imageMessage.getImageFile().getPath(),
                        options);
            }
            
            imageMessage.init(
                    getResources().getConfiguration().orientation,
                    screenWidth,
                    screenHeight);
            
            super.onLayout(changed, left, top, right, bottom);
        }
        @Override
        protected void onDraw(Canvas canvas){
            if (bitmap != null) {
                if (ImageMessage.MASK_DIMMED.equalsIgnoreCase(imageMessage.shim.mask)) {
                    canvas.drawARGB(0x66, 0x0, 0x0, 0x0);
                } else {
                    canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
                }
                
                final int orientation = getContext().getResources()
                        .getConfiguration().orientation;
                
                // draw background
                canvas.drawBitmap(
                        bitmap,
                        imageMessage.background.imageRect.asRect(),
                        imageMessage.background.layout(orientation).frame().asRect(),
                        null);
                
                // draw buttons
                final Iterator<ImageMessage.Button> buttons =
                        imageMessage.buttons();
                while (buttons.hasNext()) {
                    final ImageMessage.Button button = buttons.next();
                    canvas.drawBitmap(
                            bitmap,
                            button.imageRect.asRect(),
                            button.layout(orientation).frame().asRect(),
                            null);
                }
            } else {
                canvas.drawARGB(0x0, 0x0, 0x0, 0x0);
            }
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_UP:{
                    final int orientation = getContext().getResources()
                            .getConfiguration().orientation;
                    
                    String source = null;
                    ImageMessage.BaseAction action = null;
                    
                    // if the touch is on the popup then test buttons
                    if (imageMessage.background.layout(orientation).frame()
                            .contains((int) event.getX(), (int) event.getY())) {
                        final Iterator<ImageMessage.Button> buttons =
                                imageMessage.buttons();
                        
                        while (buttons.hasNext()) {
                            final ImageMessage.Button button = buttons.next();
                            if (button.layout(orientation).frame().contains(
                                    (int) event.getX(),
                                    (int )event.getY())) {
                                source = "button";
                                action = button.action(orientation);
                                break;
                            }
                        }
                        
                        if (action == null) {
                            source = "background";
                            action = imageMessage.background.action(orientation);
                        }
                    } else {
                        // touch is outside the popup so use shim action
                        source = "shim";
                        action = imageMessage.shim.action;
                    }
                    
                    performAction(source, action);
                }
            }
            
            return true;
        }
    }
}
