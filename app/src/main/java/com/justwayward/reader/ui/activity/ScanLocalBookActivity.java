package com.justwayward.reader.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;

import com.justwayward.reader.R;
import com.justwayward.reader.base.BaseActivity;
import com.justwayward.reader.bean.Recommend;
import com.justwayward.reader.bean.support.RefreshCollectionListEvent;
import com.justwayward.reader.component.AppComponent;
import com.justwayward.reader.manager.CollectionsManager;
import com.justwayward.reader.ui.easyadapter.RecommendAdapter;
import com.justwayward.reader.utils.FileUtils;
import com.justwayward.reader.view.recyclerview.EasyRecyclerView;
import com.justwayward.reader.view.recyclerview.adapter.RecyclerArrayAdapter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;

/**
 * 扫描本地书籍
 *
 * @author yuyh.
 * @date 2016/10/9.
 */
public class ScanLocalBookActivity extends BaseActivity implements RecyclerArrayAdapter.OnItemClickListener {

    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, ScanLocalBookActivity.class));
    }

    @Bind(R.id.recyclerview)
    EasyRecyclerView mRecyclerView;

    private RecommendAdapter mAdapter;
    private ArrayList<Recommend.RecommendBooks> list;

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan_local_book;
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {

    }

    @Override
    public void initToolBar() {
        mCommonToolbar.setTitle("扫描本地书籍");
        mCommonToolbar.setNavigationIcon(R.drawable.ab_back);
    }

    @Override
    public void initDatas() {
        list = new ArrayList<>();
    }

    @Override
    public void configViews() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemDecoration(ContextCompat.getColor(this, R.color.common_divider_narrow), 1, 0, 0);

        mAdapter = new RecommendAdapter(this);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapterWithProgress(mAdapter);

        queryFiles();
    }

    private void queryFiles() {
        String[] projection = new String[]{MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE
        };
        Cursor cursor = getContentResolver().query(
                Uri.parse("content://media/external/file"),
                projection,
                MediaStore.Files.FileColumns.DATA + " like ?",
                new String[]{"%.txt"},
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idindex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                int dataindex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                int sizeindex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
                do {
                    String path = cursor.getString(dataindex);
                    int dot = path.lastIndexOf("/");
                    String name = path.substring(dot + 1);
                    if (name.lastIndexOf(".") > 0)
                        name = name.substring(0, name.lastIndexOf("."));

                    Recommend.RecommendBooks books = new Recommend.RecommendBooks();
                    books._id = name;
                    books.path = path;
                    books.title = name;
                    books.isFromSD = true;
                    books.lastChapter = path;

                    mAdapter.add(books);

                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    @Override
    public void onItemClick(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("是否加入书架")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Recommend.RecommendBooks books = mAdapter.getItem(position);
                        // 拷贝到缓存目录
                        FileUtils.fileChannelCopy(new File(books.path),
                                new File(FileUtils.getChapterPath(books._id, 1)));
                        // 加入书架
                        CollectionsManager.getInstance().add(books);
                        // 通知
                        EventBus.getDefault().post(new RefreshCollectionListEvent());
                        dialog.dismiss();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
}
