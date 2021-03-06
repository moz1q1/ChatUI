package com.rance.chatui.ui.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.labo.kaji.relativepopupwindow.RelativePopupWindow;
import com.rance.chatui.R;
import com.rance.chatui.adapter.ChatAdapter;
import com.rance.chatui.adapter.CommonFragmentPagerAdapter;
import com.rance.chatui.adapter.OnRefreshListener;
import com.rance.chatui.base.ImApplication;
import com.rance.chatui.dao.MessageInfoDao;
import com.rance.chatui.enity.FullImageInfo;
import com.rance.chatui.enity.Link;
import com.rance.chatui.enity.MessageInfo;
import com.rance.chatui.ui.fragment.ChatEmotionFragment;
import com.rance.chatui.ui.fragment.ChatFunctionFragment;
import com.rance.chatui.util.Constants;
import com.rance.chatui.util.GlobalOnItemClickManagerUtils;
import com.rance.chatui.util.MediaManager;
import com.rance.chatui.util.MessageCenter;
import com.rance.chatui.widget.ChatContextMenu;
import com.rance.chatui.widget.EmotionInputDetector;
import com.rance.chatui.widget.NoScrollViewPager;
import com.rance.chatui.widget.StateButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 作者：Rance on 2016/11/29 10:47
 * 邮箱：rance935@163.com
 */
public class IMActivity extends AppCompatActivity {
    private static final String TAG = "IMActivity";
    RecyclerView chatList;
    ImageView emotionVoice;
    EditText editText;
    TextView voiceText;
    ImageView emotionButton;
    ImageView emotionAdd;
    StateButton emotionSend;
    NoScrollViewPager viewpager;
    RelativeLayout emotionLayout;

    private EmotionInputDetector mDetector;
    private ArrayList<Fragment> fragments;
    private ChatEmotionFragment chatEmotionFragment;
    private ChatFunctionFragment chatFunctionFragment;
    private CommonFragmentPagerAdapter adapter;

    private ChatAdapter chatAdapter;
    private LinearLayoutManager layoutManager;

    //录音相关
    int animationRes = 0;
    int res = 0;
    AnimationDrawable animationDrawable = null;
    private ImageView animView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByIds();
        EventBus.getDefault().register(this);
        initWidget();
        handleIncomeAction();
    }

    private void findViewByIds() {
        chatList = (RecyclerView) findViewById(R.id.chat_list);
        emotionVoice = (ImageView) findViewById(R.id.emotion_voice);
        editText = (EditText) findViewById(R.id.edit_text);
        voiceText = (TextView) findViewById(R.id.voice_text);
        emotionButton = (ImageView) findViewById(R.id.emotion_button);
        emotionAdd = (ImageView) findViewById(R.id.emotion_add);
        emotionSend = (StateButton) findViewById(R.id.emotion_send);
        emotionLayout = (RelativeLayout) findViewById(R.id.emotion_layout);
        viewpager = (NoScrollViewPager) findViewById(R.id.viewpager);
    }

    private void handleIncomeAction() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }

        MessageCenter.handleIncoming(bundle, getIntent().getType(), this);
    }

    private void initWidget() {
        fragments = new ArrayList<>();
        chatEmotionFragment = new ChatEmotionFragment();
        fragments.add(chatEmotionFragment);
        chatFunctionFragment = new ChatFunctionFragment();
        fragments.add(chatFunctionFragment);
        adapter = new CommonFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        viewpager.setAdapter(adapter);
        viewpager.setCurrentItem(0);

        mDetector = EmotionInputDetector.with(this)
                .setEmotionView(emotionLayout)
                .setViewPager(viewpager)
                .bindToContent(chatList)
                .bindToEditText(editText)
                .bindToEmotionButton(emotionButton)
                .bindToAddButton(emotionAdd)
                .bindToSendButton(emotionSend)
                .bindToVoiceButton(emotionVoice)
                .bindToVoiceText(voiceText)
                .build();

        GlobalOnItemClickManagerUtils globalOnItemClickListener = GlobalOnItemClickManagerUtils.getInstance(this);
        globalOnItemClickListener.attachToEditText(getLifecycle(), editText);

        chatAdapter = new ChatAdapter(null);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatList.setLayoutManager(layoutManager);
        chatList.setAdapter(chatAdapter);
        chatList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        chatAdapter.notifyDataSetChanged();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        mDetector.hideEmotionLayout(false);
                        mDetector.hideSoftInput();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });
        chatAdapter.setRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                chatList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MessageInfo messageInfo2 = new MessageInfo();
                        messageInfo2.setContent("current:" + current);
                        messageInfo2.setFileType(Constants.CHAT_FILE_TYPE_TEXT);
                        messageInfo2.setType(Constants.CHAT_ITEM_TYPE_LEFT);
                        messageInfo2.setHeader("http://img0.imgtn.bdimg.com/it/u=401967138,750679164&fm=26&gp=0.jpg");

                        ImApplication.getInstance().getDaoSession().getMessageInfoDao().insert(messageInfo2);

                        List<MessageInfo> list = ImApplication
                                .getInstance()
                                .getDaoSession()
                                .getMessageInfoDao()
                                .queryBuilder()
                                .orderAsc(MessageInfoDao.Properties.Id)
                                .build()
                                .list();

                        chatAdapter.setNewData(list);

                        if (current > total) {
                            chatAdapter.setLoadState(ChatAdapter.LOADING_END);
                        } else {
                            chatAdapter.setLoadState(ChatAdapter.LOADING_COMPLETE);
                        }
                        current++;
                    }
                }, 1000);
            }
        });
        chatAdapter.setRecyclerView(chatList);
        chatAdapter.addItemClickListener(itemClickListener);
        LoadData();
    }

    private int current = 0;

    private int total = 5;
    /**
     * item点击事件
     */
    private ChatAdapter.onItemClickListener itemClickListener = new ChatAdapter.onItemClickListener() {
        @Override
        public void onHeaderClick(int position) {
            Toast.makeText(IMActivity.this, "onHeaderClick", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onImageClick(View view, int position) {
            int location[] = new int[2];
            view.getLocationOnScreen(location);
            FullImageInfo fullImageInfo = new FullImageInfo();
            fullImageInfo.setLocationX(location[0]);
            fullImageInfo.setLocationY(location[1]);
            fullImageInfo.setWidth(view.getWidth());
            fullImageInfo.setHeight(view.getHeight());
            fullImageInfo.setImageUrl(chatAdapter.getMessageInfoList().get(position).getFilepath());
            EventBus.getDefault().postSticky(fullImageInfo);
            startActivity(new Intent(IMActivity.this, FullImageActivity.class));
            overridePendingTransition(0, 0);
        }

        @Override
        public void onVoiceClick(final ImageView imageView, final int position) {
            if (animView != null) {
                animView.setImageResource(res);
                animView = null;
            }
            switch (chatAdapter.getMessageInfoList().get(position).getType()) {
                case 1:
                    animationRes = R.drawable.voice_left;
                    res = R.mipmap.icon_voice_left3;
                    break;
                case 2:
                    animationRes = R.drawable.voice_right;
                    res = R.mipmap.icon_voice_right3;
                    break;
            }

            animView = imageView;
            animView.setImageResource(animationRes);
            animationDrawable = (AnimationDrawable) imageView.getDrawable();
            animationDrawable.start();
            MediaManager.playSound(chatAdapter.getMessageInfoList().get(position).getFilepath(), new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    animView.setImageResource(res);
                }
            });
        }

        @Override
        public void onFileClick(View view, int position) {

            MessageInfo messageInfo = chatAdapter.getMessageInfoList().get(position);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            File file = new File(messageInfo.getFilepath());
            Uri fileUri = FileProvider.getUriForFile(IMActivity.this, Constants.AUTHORITY, file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setDataAndType(fileUri, messageInfo.getMimeType());
            startActivity(intent);
        }

        @Override
        public void onLinkClick(View view, int position) {
            MessageInfo messageInfo = chatAdapter.getMessageInfoList().get(position);
            Link link = (Link) messageInfo.getObject();
            Uri uri = Uri.parse(link.getUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }

        @Override
        public void onLongClickImage(View view, int position) {

            ChatContextMenu chatContextMenu = new ChatContextMenu(view.getContext(), chatAdapter.getMessageInfoList().get(position));
//            chatContextMenu.setAnimationStyle();
            chatContextMenu.showOnAnchor(view, RelativePopupWindow.VerticalPosition.ABOVE,
                    RelativePopupWindow.HorizontalPosition.CENTER);

        }

        @Override
        public void onLongClickText(View view, int position) {
            ChatContextMenu chatContextMenu = new ChatContextMenu(view.getContext(), chatAdapter.getMessageInfoList().get(position));
            chatContextMenu.showOnAnchor(view, RelativePopupWindow.VerticalPosition.ABOVE,
                    RelativePopupWindow.HorizontalPosition.CENTER);
        }

        @Override
        public void onLongClickItem(View view, int position) {
            ChatContextMenu chatContextMenu = new ChatContextMenu(view.getContext(), chatAdapter.getMessageInfoList().get(position));
            chatContextMenu.showOnAnchor(view, RelativePopupWindow.VerticalPosition.ABOVE,
                    RelativePopupWindow.HorizontalPosition.CENTER);
        }

        @Override
        public void onLongClickFile(View view, int position) {
            ChatContextMenu chatContextMenu = new ChatContextMenu(view.getContext(), chatAdapter.getMessageInfoList().get(position));
            chatContextMenu.showOnAnchor(view, RelativePopupWindow.VerticalPosition.ABOVE,
                    RelativePopupWindow.HorizontalPosition.CENTER);
        }

        @Override
        public void onLongClickLink(View view, int position) {
            ChatContextMenu chatContextMenu = new ChatContextMenu(view.getContext(), chatAdapter.getMessageInfoList().get(position));
            chatContextMenu.showOnAnchor(view, RelativePopupWindow.VerticalPosition.ABOVE,
                    RelativePopupWindow.HorizontalPosition.CENTER);
        }
    };

    /**
     * 构造聊天数据
     */
    private void LoadData() {
        List<MessageInfo> messageInfos = new ArrayList<>();

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContent("你好，欢迎使用");
        messageInfo.setFileType(Constants.CHAT_FILE_TYPE_TEXT);
        messageInfo.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        messageInfo.setHeader("http://img0.imgtn.bdimg.com/it/u=401967138,750679164&fm=26&gp=0.jpg");
        messageInfos.add(messageInfo);

        MessageInfo messageInfo1 = new MessageInfo();
        messageInfo1.setFilepath("http://www.trueme.net/bb_midi/welcome.wav");
        messageInfo1.setVoiceTime(3000);
        messageInfo1.setFileType(Constants.CHAT_FILE_TYPE_VOICE);
        messageInfo1.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo1.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
        messageInfo1.setHeader("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fcdn-media.extratv.com%2F2012%2F11%2F28%2Fadrienne-maloof-480x360.jpg&refer=http%3A%2F%2Fcdn-media.extratv.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1616220671&t=096ed6a55855fd7bbb897915141295de");
        messageInfos.add(messageInfo1);

        MessageInfo messageInfo2 = new MessageInfo();
        messageInfo2.setFilepath("https://t7.baidu.com/it/u=3832474391,1084278450&fm=193&f=GIF");
        messageInfo2.setFileType(Constants.CHAT_FILE_TYPE_IMAGE);
        messageInfo2.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        messageInfo2.setHeader("http://img0.imgtn.bdimg.com/it/u=401967138,750679164&fm=26&gp=0.jpg");
        messageInfos.add(messageInfo2);

        MessageInfo messageInfo3 = new MessageInfo();
        messageInfo3.setContent("[微笑][色][色][色]");
        messageInfo3.setFileType(Constants.CHAT_FILE_TYPE_TEXT);
        messageInfo3.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo3.setSendState(Constants.CHAT_ITEM_SEND_ERROR);
        messageInfo3.setHeader("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fcdn-media.extratv.com%2F2012%2F11%2F28%2Fadrienne-maloof-480x360.jpg&refer=http%3A%2F%2Fcdn-media.extratv.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1616220671&t=096ed6a55855fd7bbb897915141295de");
        messageInfos.add(messageInfo3);


        MessageInfo messageInfo4 = new MessageInfo();
        messageInfo4.setContent("[色][色]][色][色][色][微笑][微笑][色]");
        messageInfo4.setFileType(Constants.CHAT_FILE_TYPE_TEXT);
        messageInfo4.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo4.setSendState(Constants.CHAT_ITEM_SEND_ERROR);
        messageInfo4.setHeader("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fcdn-media.extratv.com%2F2012%2F11%2F28%2Fadrienne-maloof-480x360.jpg&refer=http%3A%2F%2Fcdn-media.extratv.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1616220671&t=096ed6a55855fd7bbb897915141295de");
        messageInfos.add(messageInfo4);

        ImApplication.getInstance().getDaoSession().getMessageInfoDao().insertOrReplaceInTx(messageInfos);

        List<MessageInfo> list = ImApplication
                .getInstance()
                .getDaoSession()
                .getMessageInfoDao()
                .queryBuilder()
                .orderAsc(MessageInfoDao.Properties.Id)
                .build()
                .list();

        chatAdapter.setNewData(list);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEventBus(final MessageInfo messageInfo) {
        messageInfo.setHeader("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fcdn-media.extratv.com%2F2012%2F11%2F28%2Fadrienne-maloof-480x360.jpg&refer=http%3A%2F%2Fcdn-media.extratv.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1616220671&t=096ed6a55855fd7bbb897915141295de");
        messageInfo.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo.setSendState(Constants.CHAT_ITEM_SENDING);
        chatAdapter.add(messageInfo);
        chatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                messageInfo.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
                chatAdapter.notifyDataSetChanged();
            }
        }, 2000);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                MessageInfo message = new MessageInfo();
                message.setContent("这是模拟消息回复");
                message.setType(Constants.CHAT_ITEM_TYPE_LEFT);
                message.setFileType(Constants.CHAT_FILE_TYPE_TEXT);
                message.setHeader("http://img0.imgtn.bdimg.com/it/u=401967138,750679164&fm=26&gp=0.jpg");
                chatAdapter.add(message);
                chatList.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        }, 3000);
    }

    @Override
    public void onBackPressed() {
        if (!mDetector.interceptBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().removeStickyEvent(this);
        EventBus.getDefault().unregister(this);
    }
}
