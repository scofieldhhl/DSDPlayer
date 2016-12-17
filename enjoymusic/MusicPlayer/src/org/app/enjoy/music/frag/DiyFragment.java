package org.app.enjoy.music.frag;import android.content.Intent;import android.os.Bundle;import android.os.Handler;import android.os.Message;import android.support.v4.app.Fragment;import android.util.Log;import android.util.TypedValue;import android.view.LayoutInflater;import android.view.View;import android.view.ViewGroup;import android.widget.AdapterView;import android.widget.LinearLayout;import com.umeng.analytics.MobclickAgent;import org.app.enjoy.music.adapter.CategoryAdapter;import org.app.enjoy.music.adapter.MusicAdapter;import org.app.enjoy.music.data.MusicData;import org.app.enjoy.music.db.DbDao;import org.app.enjoy.music.mode.DataObservable;import org.app.enjoy.music.service.MusicService;import org.app.enjoy.music.tool.Contsant;import org.app.enjoy.music.tool.Setting;import org.app.enjoy.music.util.MusicUtil;import org.app.enjoy.music.util.SharePreferencesUtil;import org.app.enjoy.music.view.swipemenulistview.SwipeMenu;import org.app.enjoy.music.view.swipemenulistview.SwipeMenuCreator;import org.app.enjoy.music.view.swipemenulistview.SwipeMenuItem;import org.app.enjoy.music.view.swipemenulistview.SwipeMenuListView;import org.app.enjoy.musicplayer.MusicActivity;import org.app.enjoy.musicplayer.R;import java.io.Serializable;import java.util.ArrayList;import java.util.List;import java.util.Observable;import java.util.Observer;/** * Created by victor on 2016/6/12. */public class DiyFragment extends Fragment implements CategoryAdapter.OnAddMenuListener,Observer {    private String TAG = "DiyFragment";    private final int CATEGORY_SWIPE_MENU_CREATOR = 0;    private final int LIST_SWIPE_MENU_CREATOR = 1;    private final int ON_CATEGORY_MENU_ITEM_CLICK = 0;    private final int ON_LIST_MENU_ITEM_CLICK = 1;    private final int ON_CATEGORY_ITEM_CLICK = 0;    private final int ON_LIST_ITEM_CLICK = 1;    private LinearLayout mLayoutFrag;    private SwipeMenuListView mSmlvMenu,mSmlvList;    private List<String> categorys = new ArrayList<String>();    private List<MusicData> musicDatas = new ArrayList<MusicData>();    private CategoryAdapter mCategoryAdapter;    private MusicAdapter musicAdapter;    private int currentCategoryIndex = 0;    private int currentPosition = -1;    Handler mHandler = new Handler(){        public void handleMessage(Message msg) {            switch (msg.what) {                case Contsant.Msg.UPDATE_CATEGORY_LIST:                    mCategoryAdapter.setDatas(categorys);                    mCategoryAdapter.notifyDataSetChanged();                    break;                case Contsant.Msg.UPDATE_PLAY_LIST:                    if (musicAdapter != null && currentPosition < musicDatas.size()) {                        musicAdapter.setCurrentPosition(currentPosition);                        mSmlvList.setSelection(currentPosition);                    }                    break;                default:                    break;            }        };    };    @Override    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        View view = inflater.inflate(R.layout.frag_diy,container, false);        initialize(view);        initData();        return view;    }    private void initialize (View view) {        DataObservable.getInstance().addObserver(this);        mLayoutFrag = (LinearLayout) view.findViewById(R.id.ll_frag_diy);        mSmlvMenu = (SwipeMenuListView) view.findViewById(R.id.smlv_menu);        mSmlvList = (SwipeMenuListView) view.findViewById(R.id.smlv_list);        mCategoryAdapter = new CategoryAdapter(getActivity());        mCategoryAdapter.setDatas(categorys);        mSmlvMenu.setAdapter(mCategoryAdapter);        mCategoryAdapter.setOnAddMenuListener(this);        mSmlvMenu.setMenuCreator(new mSwipeMenuCreator(CATEGORY_SWIPE_MENU_CREATOR));        mSmlvMenu.setOnMenuItemClickListener(new mOnMenuItemClickListener(ON_CATEGORY_MENU_ITEM_CLICK));        //////////////////////////////////////////////////        musicAdapter = new MusicAdapter(getActivity());        musicAdapter.setDatas(musicDatas);        mSmlvList.setAdapter(musicAdapter);        // set creator        mSmlvList.setMenuCreator(new mSwipeMenuCreator(CATEGORY_SWIPE_MENU_CREATOR));        mSmlvList.setOnMenuItemClickListener(new mOnMenuItemClickListener(ON_LIST_MENU_ITEM_CLICK));        mSmlvMenu.setOnItemClickListener(new mOnItemClickListener(ON_CATEGORY_ITEM_CLICK));        mSmlvList.setOnItemClickListener(new mOnItemClickListener(ON_LIST_ITEM_CLICK));    }    private void initCategoryData () {        categorys = DbDao.getInstance(getContext()).queryCategory();        categorys.add("");        mCategoryAdapter.setDatas(categorys);        mCategoryAdapter.notifyDataSetChanged();    }    private void initMusicData (int categoryIndex) {        if (categoryIndex > categorys.size() - 1) {            categoryIndex = 0;        }        musicDatas = DbDao.getInstance(getContext()).queryMusicByCategory(categorys.get(categoryIndex));        musicAdapter.setDatas(musicDatas);        musicAdapter.notifyDataSetChanged();    }    private void initData() {        // TODO Auto-generated method stub        initCategoryData();        initMusicData(currentCategoryIndex);    }    private int dp2px(int dp) {        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,                getResources().getDisplayMetrics());    }    @Override    public void update(Observable observable, Object data) {        if (data instanceof Bundle) {            Bundle bundle = (Bundle) data;            int action = bundle.getInt(Contsant.ACTION_KEY);            int position = bundle.getInt(Contsant.POSITION_KEY);            if (action == Contsant.Action.POSITION_CHANGED) {//后台发过来的播放位置改变前台同步改变                if (((MusicActivity) getActivity()).getCurrentPage() == Contsant.Frag.ALBUM_FRAG) {                    int currentPlayFrag = SharePreferencesUtil.getInt(getContext(), Contsant.CURRENT_FRAG);                    if (currentPlayFrag != Contsant.Frag.ALBUM_FRAG) {                        String currentMusicName = SharePreferencesUtil.getString(getContext(), Contsant.CURRENT_MUSIC_NAME);                        position = MusicUtil.getPositionByMusicName(musicDatas,currentMusicName);                    }                    currentPosition = position;                    mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);                }            }        } else if (data instanceof Integer) {            int currentFrag = (int)data;            if (currentFrag == Contsant.Frag.DIY_FRAG) {                if (musicDatas != null && musicDatas.size() > 0) {                    String currentMusicName = SharePreferencesUtil.getString(getContext(),Contsant.CURRENT_MUSIC_NAME);                    int index = MusicUtil.getPositionByMusicName(musicDatas,currentMusicName);                    if (currentPosition != index) {                        currentPosition = index;                        mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_PLAY_LIST);                    }                }            }        }    }    class mSwipeMenuCreator implements SwipeMenuCreator {        private int mAction;        public mSwipeMenuCreator (int action) {            mAction = action;        }        @Override        public void create(SwipeMenu menu) {            // TODO Auto-generated method stub            SwipeMenuItem deleteItem;            switch (mAction) {                case CATEGORY_SWIPE_MENU_CREATOR:                    deleteItem = new SwipeMenuItem(getActivity());                    // set item background                    deleteItem.setBackground(R.color.light_grey);                    // set item width                    deleteItem.setWidth(dp2px(50));                    // set a icon                    deleteItem.setIcon(R.drawable.ic_delete);                    // add to menu                    menu.addMenuItem(deleteItem);                    break;                case LIST_SWIPE_MENU_CREATOR:                    deleteItem = new SwipeMenuItem(getActivity());                    // set item background                    deleteItem.setBackground(R.color.light_grey);                    // set item width                    deleteItem.setWidth(dp2px(50));                    // set a icon                    deleteItem.setIcon(R.drawable.ic_delete);                    // add to menu                    menu.addMenuItem(deleteItem);                    break;                default:                    break;            }        }    }    class mOnMenuItemClickListener implements SwipeMenuListView.OnMenuItemClickListener {        private int mAction;        public mOnMenuItemClickListener (int action) {            mAction = action;        }        @Override        public void onMenuItemClick(int position, SwipeMenu menu, int index) {            // TODO Auto-generated method stub            switch (mAction) {                case ON_CATEGORY_MENU_ITEM_CLICK:                    //第一个和最后一个分类不允许删除                    if (position == 0 || position == 1 || position == categorys.size() - 1) {                        return;                    }                    DbDao.getInstance(getContext()).removeCategory(categorys.get(position));                    initCategoryData();                    break;                case ON_LIST_MENU_ITEM_CLICK:                    MusicUtil.deleteFile(getContext(),musicDatas.get(position).data,musicDatas.get(position).id);                    DbDao.getInstance(getContext()).removeMusic(musicDatas.get(position).id);                    initMusicData(currentCategoryIndex);                    break;                default:                    break;            }        }    }    class mOnItemClickListener implements AdapterView.OnItemClickListener {        private int mAction;        public mOnItemClickListener (int action) {            mAction = action;        }        @Override        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {            switch (mAction) {                case ON_CATEGORY_ITEM_CLICK:                    currentCategoryIndex = position;                    musicDatas = DbDao.getInstance(getContext()).queryMusicByCategory(categorys.get(position));                    musicAdapter.setDatas(musicDatas);                    String currentMusicName = SharePreferencesUtil.getString(getContext(),Contsant.CURRENT_MUSIC_NAME);                    currentPosition = MusicUtil.getPositionByMusicName(musicDatas,currentMusicName);                    musicAdapter.setCurrentPosition(currentPosition);                    break;                case ON_LIST_ITEM_CLICK:                    SharePreferencesUtil.putInt(getContext(), Contsant.CURRENT_FRAG, Contsant.Frag.DIY_FRAG);                    currentPosition = position;                    if (musicAdapter != null) {                        musicAdapter.setCurrentPosition(currentPosition);                    }                    play(musicDatas);//                    Bundle bundle = new Bundle();//                    bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);//                    bundle.putInt(Contsant.POSITION_KEY, currentPosition);////                    Intent intent = new Intent();//                    intent.setAction(Contsant.PlayAction.MUSIC_LIST);//                    intent.putExtras(bundle);//                    getActivity().sendBroadcast(intent);                    Bundle bundle = new Bundle();                    bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);                    bundle.putInt(Contsant.ACTION_KEY, Contsant.Action.MUSIC_LIST_ITEM_CLICK);                    bundle.putInt(Contsant.POSITION_KEY, currentPosition);                    DataObservable.getInstance().setData(bundle);                    break;            }        }    }    @Override    public void onAddMenu() {        Log.e(TAG, "onAddMenu()..........................");        categorys.remove(categorys.size() - 1);        String category = getActivity().getString(R.string.diy_custom) + (categorys.size());        DbDao.getInstance(getContext()).addCategory(category);        initCategoryData();        mHandler.sendEmptyMessage(Contsant.Msg.UPDATE_CATEGORY_LIST);    }    public void play(List<MusicData> musicDatas) {        Intent intent = new Intent();        Bundle bundle = new Bundle();        bundle.putSerializable(Contsant.MUSIC_LIST_KEY, (Serializable) musicDatas);        bundle.putInt(Contsant.POSITION_KEY, currentPosition);        intent.putExtras(bundle);        intent.setAction("com.app.media.MUSIC_SERVICE");        intent.putExtra("op", 1);// 向服务传递数据        intent.setPackage(getActivity().getPackageName());        getActivity().startService(intent);    }    @Override    public void onResume() {        super.onResume();        MobclickAgent.onResume(getActivity());    }    public void onPause() {        super.onPause();        MobclickAgent.onPause(getActivity());    }    @Override    public void onDestroy() {        DataObservable.getInstance().deleteObserver(this);        super.onDestroy();    }}