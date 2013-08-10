package com.van2k.sample.van2kbros;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.van2k.gamesdk.Van2k;
import com.van2k.gamesdk.Van2kListener;

public class MyActivity extends Activity
{
    public final static String TAG = "Van2kBros";

    // Van2k settings
    private final static String VAN2K_API_KEY = "4In6ybnSuM7lCrQAKaFOdnPMb1hN3xdi";
    private final static String VAN2K_DEVELOPER_ID = "XHw8uKbp0NiwrOjeCiFWiYeQPDaawlKH";
    private final static int VAN2K_APP_ID = 1;
    public final static int VAN2K_RANKING_ID = 1;

    // 退会メニュー
    private static final int MENU_ID_WITHDRAW = (Menu.FIRST + 1);

    public Van2k van2k;
    private boolean bVan2kLoggedIn = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int versionCode = 0;
        try {
            versionCode = this.getPackageManager().getPackageInfo(
                    this.getPackageName(), 1).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        van2k = new Van2k(this, VAN2K_API_KEY, VAN2K_DEVELOPER_ID, VAN2K_APP_ID, versionCode, new Van2kListener() {
            @Override
            public void onLogin(boolean b) {
                if (b){
                    Log.d(TAG, "Logged in successfully.");
                    bVan2kLoggedIn = true;
                } else {
                    Log.d(TAG, "Login failed.");
                }
            }
        });

        setContentView(new GameView(this));
    }

    // オプションメニューが最初に呼び出される時に1度だけ呼び出されます
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューアイテムを追加します
        menu.add(Menu.NONE, MENU_ID_WITHDRAW, Menu.NONE, "番付を退会する");
        return super.onCreateOptionsMenu(menu);
    }

    // オプションメニューが表示される度に呼び出されます
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_ID_WITHDRAW).setVisible(bVan2kLoggedIn);
        return super.onPrepareOptionsMenu(menu);
    }

    // オプションメニューアイテムが選択された時に呼び出されます
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret;
        switch (item.getItemId()) {
            case MENU_ID_WITHDRAW:  // 退会
                van2k.withdraw();
                ret = true;
                break;
            default:
                ret = super.onOptionsItemSelected(item);
                break;
        }
        return ret;
    }
}
