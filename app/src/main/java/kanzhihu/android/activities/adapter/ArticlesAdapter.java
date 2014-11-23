package kanzhihu.android.activities.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.picasso.Picasso;
import de.greenrobot.event.EventBus;
import java.util.List;
import kanzhihu.android.App;
import kanzhihu.android.AppConstant;
import kanzhihu.android.R;
import kanzhihu.android.activities.adapter.base.ParallaxRecyclerAdapter;
import kanzhihu.android.events.ListitemClickEvent;
import kanzhihu.android.events.MarkChangeEvent;
import kanzhihu.android.models.Article;

/**
 * Created by Jiahui.wen on 2014/11/15.
 */
public class ArticlesAdapter extends ParallaxRecyclerAdapter<Article>
    implements ParallaxRecyclerAdapter.RecyclerAdapterMethods {

    public ArticlesAdapter(List<Article> data) {
        super(data);
        implementRecyclerAdapterMethods(this);
    }

    @Override public RecyclerView.ViewHolder onCreateViewHolderImpl(ViewGroup viewGroup, int i) {
        return new ArticleHolder(
            LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.listitem_article, viewGroup, false));
    }

    @Override public void onBindViewHolderImpl(RecyclerView.ViewHolder viewHolder, int i) {
        Article article = getItem(i);
        ArticleHolder holder = (ArticleHolder) viewHolder;

        holder.mTitle.setText(article.title);
        holder.mContent.setText(article.summary);
        holder.mAuthor.setText(article.writer);
        holder.mAgree.setText(String.valueOf(article.agreeCount));

        holder.unRegisterCheckedChangedListener();
        holder.mMarked.setChecked(article.marked > 0);
        holder.registerCheckedChangedListener();

        Picasso.with(App.getAppContext())
            .load(String.format(AppConstant.IMAGE_LINK, article.imageLink))
            .into(holder.mAvatar);
    }

    @Override public int getItemCountImpl() {
        return getData() == null ? 0 : getData().size();
    }

    public static class ArticleHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        @InjectView(R.id.tv_title)
        public TextView mTitle;

        @InjectView(R.id.tv_content)
        public TextView mContent;

        @InjectView(R.id.tv_author)
        public TextView mAuthor;

        @InjectView(R.id.tv_agree)
        public TextView mAgree;

        @InjectView(R.id.iv_article_img)
        public ImageView mAvatar;

        @InjectView(R.id.cb_mark)
        public CheckBox mMarked;

        public ArticleHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void registerCheckedChangedListener() {
            mMarked.setOnCheckedChangeListener(this);
        }

        public void unRegisterCheckedChangedListener() {
            mMarked.setOnCheckedChangeListener(null);
        }

        @Override public void onClick(View v) {
            EventBus.getDefault().post(new ListitemClickEvent(getPosition()));
        }

        @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            EventBus.getDefault().post(new MarkChangeEvent(getPosition(), isChecked));
        }
    }
}
