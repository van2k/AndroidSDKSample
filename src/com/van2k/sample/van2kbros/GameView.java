package com.van2k.sample.van2kbros;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.content.res.Resources;
import android.graphics.*;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.van2k.gamesdk.ResultListener;
import com.van2k.gamesdk.Van2k;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private enum STATE {
        TITLE, IN_GAME, GAME_OVER, DISPLAY_RANKING
    }
    private STATE state = STATE.TITLE;

    private MyActivity mActivity;

    private String[] title_button_text = {"Start game", "Ranking"};
    private Rect[] title_button_rect = new Rect[2];

	private float droid_x = 0;	// 自キャラのX座標
	private float droid_y = 0;	// 自キャラのY座標
	private int droid_jumping;	// ジャンプ状態：0=ジャンプしていない 1=1段目中 2=2段目中
	private float droid_vy;		// 縦方向速度

	private LinkedList<EnemyInfo> enemy_list =
		new LinkedList<EnemyInfo>();
	private Random random = new Random();

	private float ground_y;		// 地面のY座標
	private boolean touch_down;	// タッチした瞬間true

	private Timer mTimer = null;
	private float cloud_x;
	private float cloud_y;

	private int score;				// 点数

	private Paint paint = new Paint();

	private Bitmap bmpDroid;
	private Bitmap[] bmpEnemy = new Bitmap[3];

    private Handler mHandler;

    // 敵キャラの状態を表すクラス
    private class EnemyInfo {
		public int type;		// 敵タイプ(0～2)
		public float pos_x;	// X座標
		public float pos_y;	// Y座標
		public float width;	// 幅
		public float height;	// 高さ
		public float vx;		// 横方向速度
		public float vy;		// 縦方向速度

		public EnemyInfo(int enemy_type){
			// 出現時の状態にする
			type = enemy_type;
			width = bmpEnemy[type].getWidth();
			height = bmpEnemy[type].getHeight();
			pos_x = getWidth();	// 画面右端に出現
			pos_y = ground_y - height;

			switch(type){
			case 0 :
				vx = -10;
				vy = 0;
				break;
			case 1 :
				pos_y = ground_y - height;
				vx = -7;
				vy = -20;
				break;
			case 2 :
				pos_y -= 100;
				vx = -5;
				vy = -5;
				break;
			}
		}

		public void onNextFrame(){
			// 30分の1秒ごとの座標の更新
			pos_x += vx;
			pos_y += vy;

			switch(type){
			case 1 :
				if (pos_y + height >= ground_y){
					// 着地した。ジャンプしよう！
					vy = -20;
				} else {
					vy += 1.5;	// 重力加速度
				}
				break;
			case 2 :
				// バネのような運動にしてみる
				vy += (ground_y - height - 100 - pos_y) * 0.1;
				break;
			}
		}
	}

	public GameView(MyActivity activity) {
		super(activity);
        mActivity = activity;
        mHandler = new Handler();

		getHolder().addCallback(this);

	    	// 画像リソースの読み込み
		Resources r = activity.getResources();
		bmpDroid = BitmapFactory.decodeResource(r, R.drawable.droid);
		bmpEnemy[0] = BitmapFactory.decodeResource(r, R.drawable.enemy1);
		bmpEnemy[1] = BitmapFactory.decodeResource(r, R.drawable.enemy2);
		bmpEnemy[2] = BitmapFactory.decodeResource(r, R.drawable.enemy3);

		// ペイントオブジェクトの初期化
		// なめらかな描画をするためにアンチエイリアスを有効にする
		paint.setAntiAlias(true);
	}

    // タイトル画面の描画
    private void drawTitleScreen(Canvas canvas){
        // 背景を水色にする
        canvas.drawColor(Color.rgb(0, 200, 255));

        // 赤い四角形を描く
        paint.setColor(Color.rgb(160, 0, 0));	// 色の指定
        paint.setStyle(Paint.Style.FILL);	// スタイル（塗りつぶし）の指定
        canvas.drawRect(0, ground_y, getWidth(), getHeight(), paint);

        // タイトル描画
        final String text = "Van2k Brothers";
        Rect rect = new Rect();
        paint.setColor(Color.BLUE);
        paint.setTextSize(50);
        paint.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text, (getWidth() - rect.width()) / 2, getHeight() / 4, paint);

        // アチーブメント描画
        paint.setTextSize(20);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        if (mActivity.van2k.hasAchievementGiven(1)){
            paint.setColor(Color.RED);
            canvas.drawText("○", 20, 0 - fontMetrics.ascent, paint);
        }
        if (mActivity.van2k.hasAchievementGiven(2)){
            paint.setColor(Color.GREEN);
            canvas.drawText("◎", 50, 0 - fontMetrics.ascent, paint);
        }

        // ボタン
        paint.setTextSize(30);
        fontMetrics = paint.getFontMetrics();
        for(int i = 0; i < title_button_rect.length; i++){
            String button_text = title_button_text[i];
            Rect button_rect = title_button_rect[i];

            paint.setColor(Color.WHITE);
            canvas.drawRect(button_rect, paint);
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(button_rect.left + 2, button_rect.top + 2, button_rect.right - 2, button_rect.bottom - 2, paint);

            paint.setColor(Color.WHITE);
            paint.getTextBounds(button_text, 0, button_text.length(), rect);
            canvas.drawText(button_text, button_rect.left + (button_rect.width() - rect.width()) / 2,
                    button_rect.top + button_rect.height() / 2 - fontMetrics.ascent / 2, paint);
        }
    }

    // ゲーム画面の描画
	private void drawGameScreen(Canvas canvas) {
		// 背景を水色にする
		canvas.drawColor(Color.rgb(0, 200, 255));

		// 赤い四角形を描く
		paint.setColor(Color.rgb(160, 0, 0));	// 色の指定
		paint.setStyle(Paint.Style.FILL);	// スタイル（塗りつぶし）の指定
		canvas.drawRect(0, ground_y, getWidth(), getHeight(), paint);

		// 白い楕円形を描く
		paint.setColor(Color.WHITE);
		canvas.drawOval(new RectF(cloud_x, cloud_y, cloud_x + 100, cloud_y + 30), paint);

		// 文字列を描く
		paint.setColor(Color.BLACK);
		paint.setTextSize(20);
		Paint.FontMetrics fontMetrics = paint.getFontMetrics();
		canvas.drawText("スコア：" + score, 0, 0 - fontMetrics.ascent, paint);

		// 画像（ドロイド君）を描く
		canvas.drawBitmap(bmpDroid, droid_x, droid_y, paint);

		// 敵を描く
		for(EnemyInfo e : enemy_list){
			canvas.drawBitmap(bmpEnemy[e.type], e.pos_x, e.pos_y, paint);
		}
	}

	@Override
	public synchronized boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();	// X座標
		float y = event.getY();	// Y座標

		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN :	// タッチされた
			// ゲームオーバー状態だったらタイトル画面へ
			if (state == STATE.GAME_OVER){
				state = STATE.TITLE;
                repaint();
				break;
			}
            // タイトル画面だったらボタンに反応させる
            if (state == STATE.TITLE){
                for(int i = 0; i < title_button_rect.length; i++){
                    Rect rect = title_button_rect[i];
                    if (rect.contains((int)x, (int)y)){
                        onTitleButton(i);
                        break;
                    }
                }
                break;
            }
			touch_down = true;
			break;
		case MotionEvent.ACTION_MOVE :	// タッチ状態のまま指が動いた
			break;
		case MotionEvent.ACTION_UP :		// 指が離れた
			break;
		case MotionEvent.ACTION_CANCEL :	// モーションがキャンセルされた
			break;
		case MotionEvent.ACTION_OUTSIDE :	// タッチ領域から外に指が動いた
			break;
		}
		return true;	// イベントを処理した場合trueを返す
	}

    // タイトル画面のボタンが押されたときに呼ばれる
    private void onTitleButton(int button_number){
        switch (button_number){
            case 0 :    // ゲーム開始
                startGame();
                break;
            case 1 :    // ランキング
                mActivity.van2k.showRanking(MyActivity.VAN2K_RANKING_ID, Van2k.TermType.TOTAL,
                        0, Van2k.CollectionType.TOTAL);
                break;
        }
    }

	private void repaint(){
		// 再描画処理
		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();
        if (canvas != null){
            if (state == STATE.TITLE){
                drawTitleScreen(canvas);
            } else {
                drawGameScreen(canvas);
            }
            holder.unlockCanvasAndPost(canvas);
        }
	}

	@Override
	public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 地面のY座標
        ground_y = getHeight() / 2;

        // タイトル画面のボタン位置を設定
        final int w = 200;
        final int h = 50;
        final int x = width / 2 - w / 2;
        int y = height / 2 + 10;
        for(int i = 0; i < title_button_rect.length; i++){
            title_button_rect[i] = new Rect(x, y, x + w, y + h);
            y += h * 2;
        }

        repaint();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// サーフェスが作られると呼ばれる
	}

	@Override
	public synchronized void surfaceDestroyed(SurfaceHolder holder) {
		// サーフェスがなくなると呼ばれる
		if (mTimer != null){
			mTimer.cancel();	// タイマー破棄
			mTimer = null;
		}
	}

	private void startGame(){
        Log.d(MyActivity.TAG, "Start game.");

		// ゲーム状態初期化
		state = STATE.IN_GAME;

		// 点数の初期化
		score = 0;

		// 敵のリストをクリア
		enemy_list.clear();

		// 自キャラの状態
		droid_vy = 0;
		droid_jumping = 0;

		// 自キャラの初期位置
		droid_x = getWidth() / 4;
		droid_y = getHeight() / 2 - bmpDroid.getHeight();

		// 操作情報の初期化
		touch_down = false;

		// 雲の初期位置
		cloud_x = getWidth();
		cloud_y = 50;

		// 画面を描画する
		repaint();

		// タイマー開始
		mTimer = new Timer();
		mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				onNextFrame();
			}
		}, 33, 33);	// 33ミリ秒間のタイマー
	}

	private synchronized void onNextFrame(){
        if (state != STATE.IN_GAME) return;

		// 敵の移動
		Iterator<EnemyInfo> itr = enemy_list.iterator();
		while(itr.hasNext()){
			EnemyInfo e = itr.next();
			e.onNextFrame();
			// 画面外に出たら、この敵は消す
			if (e.pos_x < 0) itr.remove();
		}
		// 一定の確率で敵を出現させる
		int rand = random.nextInt(50);	// 0～49の乱数
		if (rand < 3){
			enemy_list.add(new EnemyInfo(rand));
		}

		// 自キャラの移動
		if (droid_jumping > 0){
			// ジャンプ中
			// 2段ジャンプの判定
			if (touch_down && droid_jumping == 1){
				if (Math.abs(droid_vy) < 2){
					droid_jumping = 2;
					droid_vy = -15;
				}
			}
			droid_vy += 1.5;	// 重力加速度
			droid_y += droid_vy;
			if (droid_y + bmpDroid.getHeight() >= ground_y){
				// 着地した
				droid_y = ground_y - bmpDroid.getHeight();
				droid_jumping = 0;
			}
		} else {
			// ジャンプしていない
			if (touch_down){
				// タッチした。ジャンプしよう！
				droid_jumping = 1;
				droid_y -= 20;
				droid_vy = -20;
			}
	 	}
		// フラグをクリア
		touch_down = false;

		// 雲の移動
		cloud_x -= 3;
		if (cloud_x + 100 < 0){
			cloud_x = getWidth();
		}

		// 点数追加
		score++;

		// 当たり判定
		float droid_cx = droid_x + bmpDroid.getWidth() / 2;
		float droid_cy = droid_y + bmpDroid.getHeight() / 2;
		itr = enemy_list.iterator();
		while(itr.hasNext()){
			EnemyInfo e = itr.next();

			if (Math.abs((e.pos_x + e.width / 2) - droid_cx) < 30
			&& Math.abs((e.pos_y + e.height / 2) - droid_cy) < 30){
				// 敵とぶつかった！
				if (droid_y < e.pos_y){
					// 踏んづけた！
					itr.remove();	// この敵を消す
					score += 100;	// 100点増加
					droid_vy = -7;	// 少し浮かす
				} else {
					// ゲームオーバー！
					state = STATE.GAME_OVER;
				}
			}
		}
		if (state == STATE.GAME_OVER){	// 死んだらタイマー停止
			mTimer.cancel();
			mTimer = null;
            sendScore();
		}

		// 再描画
		repaint();
	}

    // スコア送信
    private void sendScore(){
        final int send_score = score;

        // ランキングへ登録
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mActivity.van2k.sendScore(MyActivity.VAN2K_RANKING_ID, send_score, new ResultListener() {
                    @Override
                    public void onFinishProcess(ResultListener.Result result) {
                        Log.d(MyActivity.TAG, "sendScore result: " + result.name());
                    }
                });
            }
        });

        // アチーブメントの授与
        if (send_score >= 1000) mActivity.van2k.giveAchievement(1, null);
        if (send_score >= 2000) mActivity.van2k.giveAchievement(2, null);
    }
}

