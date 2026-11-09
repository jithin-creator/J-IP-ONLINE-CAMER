package com.jj.ipcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.SharedPreferences;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {

    private final int GOLD = Color.rgb(214,173,82);
    private final int BG = Color.rgb(12,12,14);
    private final int PANEL = Color.rgb(30,29,27);

    private SharedPreferences prefs;
    private ExoPlayer[] players = new ExoPlayer[16];
    private PlayerView[] videoViews = new PlayerView[16];
    private TextView[] labels = new TextView[16];

    private String[] names = new String[16];
    private String[] urls = new String[16];

    private GridLayout grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("JJ_CAMERAS", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView title = new TextView(this);
        title.setText("JJ IP ONLINE CAMERA");
        title.setTextColor(GOLD);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8,20,8,12);
        title.setTypeface(null, 1);

        root.addView(title);

        TextView status = new TextView(this);
        status.setText("16 CHANNEL CCTV MONITOR");
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(5,5,5,12);
        root.addView(status);

        Button add = new Button(this);
        add.setText("+ ADD CAMERA");
        add.setTextColor(Color.BLACK);
        add.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(GOLD)
        );

        root.addView(add);

        ScrollView scroll = new ScrollView(this);

        grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setPadding(5,8,5,8);

        for (int i=0; i<16; i++) {
            createCameraTile(i);
        }

        scroll.addView(grid);
        root.addView(scroll,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,0,1
            )
        );

        add.setOnClickListener(v -> {
            for (int i=0; i<16; i++) {
                if (urls[i] == null || urls[i].isEmpty()) {
                    showCameraDialog(i);
                    return;
                }
            }
            Toast.makeText(this,
                "All 16 slots are occupied",
                Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }

    private void createCameraTile(int index) {

        names[index] = prefs.getString("name"+index,"");
        urls[index] = prefs.getString("url"+index,"");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(PANEL);
        card.setPadding(5,5,5,5);

        TextView label = new TextView(this);
        label.setTextColor(GOLD);
        label.setTextSize(13);
        label.setPadding(4,7,4,7);

        labels[index] = label;

        PlayerView playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setUseController(true);

        videoViews[index] = playerView;

        card.addView(label);

        card.addView(playerView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,190
            )
        );

        TextView hint = new TextView(this);
        hint.setText("Tap: Play / Configure   Long press: Edit");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(10);
        card.addView(hint);

        GridLayout.LayoutParams lp =
            new GridLayout.LayoutParams();

        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(index % 2,1f);
        lp.rowSpec = GridLayout.spec(index / 2);
        lp.setMargins(4,4,4,4);

        grid.addView(card,lp);

        card.setOnClickListener(v -> {
            if (urls[index] == null || urls[index].isEmpty()) {
                showCameraDialog(index);
            } else {
                startCamera(index);
            }
        });

        card.setOnLongClickListener(v -> {
            showCameraDialog(index);
            return true;
        });

        updateLabel(index);
    }

    private void updateLabel(int i) {
        String name = names[i] == null || names[i].isEmpty()
            ? "CAMERA " + (i+1) : names[i];

        labels[i].setText("● " + name);
    }

    private void showCameraDialog(int index) {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(25,15,25,5);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Camera name");
        nameInput.setSingleLine(true);
        nameInput.setText(names[index]);

        EditText urlInput = new EditText(this);
        urlInput.setHint("rtsp://camera-address:554/stream");
        urlInput.setSingleLine(true);
        urlInput.setInputType(
            android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_VARIATION_URI
        );
        urlInput.setText(urls[index]);

        form.addView(nameInput);
        form.addView(urlInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("CAMERA " + (index+1))
            .setView(form)
            .setPositiveButton("SAVE",null)
            .setNeutralButton("DELETE",null)
            .setNegativeButton("CANCEL",null)
            .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {

                    String name = nameInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();

                    if (!url.isEmpty() &&
                        !url.toLowerCase().startsWith("rtsp://")) {
                        urlInput.setError("Enter a valid RTSP URL");
                        return;
                    }

                    releaseCamera(index);

                    names[index] = name;
                    urls[index] = url;

                    prefs.edit()
                        .putString("name"+index,name)
                        .putString("url"+index,url)
                        .apply();

                    updateLabel(index);
                    dialog.dismiss();
                });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(b -> {

                    releaseCamera(index);
                    names[index] = "";
                    urls[index] = "";

                    prefs.edit()
                        .remove("name"+index)
                        .remove("url"+index)
                        .apply();

                    updateLabel(index);
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void startCamera(int index) {

        if (urls[index] == null || urls[index].isEmpty()) {
            showCameraDialog(index);
            return;
        }

        releaseCamera(index);

        try {
            ExoPlayer player =
                new ExoPlayer.Builder(this).build();

            players[index] = player;

            videoViews[index].setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    Toast.makeText(MainActivity.this,
                        "Camera " + (index+1) + " connection failed",
                        Toast.LENGTH_LONG).show();
                }
            });

            MediaItem item =
                MediaItem.fromUri(urls[index]);

            player.setMediaItem(item);
            player.prepare();
            player.play();

            Toast.makeText(this,
                "Connecting Camera " + (index+1),
                Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            releaseCamera(index);
            Toast.makeText(this,
                "Unable to start camera",
                Toast.LENGTH_LONG).show();
        }
    }

    private void releaseCamera(int index) {

        if (players[index] != null) {
            players[index].release();
            players[index] = null;
        }

        if (videoViews[index] != null) {
            videoViews[index].setPlayer(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (int i=0; i<16; i++) {
            releaseCamera(i);
        }
    }

    @Override
    protected void onDestroy() {
        for (int i=0; i<16; i++) {
            releaseCamera(i);
        }
        super.onDestroy();
    }
}
