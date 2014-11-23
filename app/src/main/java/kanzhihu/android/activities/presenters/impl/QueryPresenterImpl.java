package kanzhihu.android.activities.presenters.impl;

import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.MenuItem;
import de.greenrobot.event.EventBus;
import kanzhihu.android.App;
import kanzhihu.android.AppConstant;
import kanzhihu.android.R;
import kanzhihu.android.activities.presenters.QueryPresenter;
import kanzhihu.android.activities.views.QueryView;
import kanzhihu.android.database.ZhihuProvider;
import kanzhihu.android.database.table.ArticleTable;
import kanzhihu.android.events.ListitemClickEvent;
import kanzhihu.android.events.MarkChangeEvent;
import kanzhihu.android.jobs.SimpleBackgroundTask;
import kanzhihu.android.models.Article;
import kanzhihu.android.utils.AssertUtils;
import kanzhihu.android.utils.Cache;
import kanzhihu.android.utils.ToastUtils;

/**
 * Created by Jiahui.wen on 2014/11/20.
 */
public class QueryPresenterImpl implements QueryPresenter {

    private QueryView mView;

    String mCurFilter;

    private boolean backPressed = false;

    private SearchView.OnQueryTextListener mQueryTextListener;

    private MenuItemCompat.OnActionExpandListener mActionExpandListener;

    public QueryPresenterImpl(QueryView mView) {
        this.mView = AssertUtils.requireNonNull(mView, QueryView.class.getSimpleName() + " must not null");
    }

    @Override public void init() {

    }

    @Override public void bindEvent() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override public void unBindEvent() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    public void onEventMainThread(ListitemClickEvent event) {
        if (mView.getVisiable()) {
            mView.showArticle(event.position);
        }
    }

    public void onEventMainThread(MarkChangeEvent event) {
        if (!mView.getVisiable()) {
            return;
        }
        Article article = mView.getArticle(event.position);
        if (article != null) {
            markArticleChanged(event.position, article, event.isChecked);
        }
    }

    @Override public void markArticleChanged(final int position, final Article article, final boolean isChecked) {
        new SimpleBackgroundTask<Boolean>(mView.getContext()) {
            @Override protected Boolean onRun() {
                ContentValues values = new ContentValues(1);
                values.put(ArticleTable.MARKED, isChecked ? 1 : 0);
                int count = App.getAppContext()
                    .getContentResolver()
                    .update(Uri.parse(ZhihuProvider.ARTICLE_CONTENT_URI + "/" + article.id), values, null, null);
                return count > 0;
            }

            @Override protected void onSuccess(Boolean result) {
                if (result) {
                    Cache.remove(article);
                    article.marked = isChecked ? 1 : 0;
                    mView.articleChanged(position);
                } else {
                    ToastUtils.showShort(R.string.mark_fail);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override public void loadInitData() {
        mView.getLoaderManager().initLoader(AppConstant.SEARCH_LOADER_ID, null, QueryPresenterImpl.this);
    }

    @Override public SearchView.OnQueryTextListener getQueryTextListener() {
        if (mQueryTextListener == null) {
            mQueryTextListener = new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextChange(String newText) {
                    String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
                    if (mCurFilter == null && newFilter == null) {
                        return true;
                    }
                    if (mCurFilter != null && mCurFilter.equals(newFilter)) {
                        return true;
                    }
                    if (newFilter != null) {
                        newFilter = newFilter.trim();
                    }
                    mCurFilter = newFilter;
                    //通知adapter，查询关键字已改变
                    mView.onQueryTextChange(mCurFilter);
                    mView.getLoaderManager().restartLoader(AppConstant.SEARCH_LOADER_ID, null, QueryPresenterImpl.this);
                    return true;
                }

                @Override public boolean onQueryTextSubmit(String query) {
                    return true;
                }
            };
        }
        return mQueryTextListener;
    }

    @Override public MenuItemCompat.OnActionExpandListener getActionExpandListener() {
        if (mActionExpandListener == null) {
            mActionExpandListener = new MenuItemCompat.OnActionExpandListener() {
                @Override public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    return true;
                }

                @Override public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    if (!backPressed) {
                        backPressed = true;
                        mView.onSearchViewClosed(menuItem);
                    }
                    return true;
                }
            };
        }
        return mActionExpandListener;
    }

    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selectionArgs = null;
        if (mCurFilter != null && !mCurFilter.trim().equals("")) {
            selection = AppConstant.SEARCH_SQL_SELECTION;
            String selectionArg = "%" + mCurFilter + "%";
            selectionArgs = new String[] { selectionArg, selectionArg };
        }
        String sortOrder = ArticleTable.CATEGORY_ID + " desc, " + ArticleTable.AGREE_COUNT + " desc";

        return new CursorLoader(mView.getContext(), ZhihuProvider.ARTICLE_CONTENT_URI, null, selection, selectionArgs,
            sortOrder);
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mView.swapCursor(data);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        mView.swapCursor(null);
    }
}
