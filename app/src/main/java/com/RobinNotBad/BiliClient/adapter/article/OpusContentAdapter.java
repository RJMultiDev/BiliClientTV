package com.RobinNotBad.BiliClient.adapter.article;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.ImageViewerActivity;
import com.RobinNotBad.BiliClient.activity.user.info.UserInfoActivity;
import com.RobinNotBad.BiliClient.api.ArticleApi;
import com.RobinNotBad.BiliClient.model.Opus;
import com.RobinNotBad.BiliClient.model.OpusParagraph;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;

//文章内容Adapter by RobinNotBad

public class OpusContentAdapter extends RecyclerView.Adapter<OpusContentAdapter.ArticleLineHolder> {

    final Activity context;
    final Opus article;
    final OpusParagraph[] paragraphs;

    private int coinAdd = 0;

    public OpusContentAdapter(Activity context, Opus article) {
        this.context = context;
        this.article = article;
        this.paragraphs = article.paragraphs;
    }

    @NonNull
    @Override
    public ArticleLineHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {    //-1=头，0=文本，1=图片
            case OpusParagraph.TYPE_PIC:
            case OpusParagraph.TYPE_DIVIDER:
                view = LayoutInflater.from(this.context).inflate(R.layout.cell_article_image, parent, false);
                break;
            case -1:
                view = LayoutInflater.from(this.context).inflate(R.layout.cell_article_head, parent, false);
                break;
            case -2:
                view = LayoutInflater.from(this.context).inflate(R.layout.cell_article_end, parent, false);
                break;
            case OpusParagraph.TYPE_TEXT:
            case OpusParagraph.TYPE_TEXT_REGULAR:
            default:
                view = LayoutInflater.from(this.context).inflate(R.layout.cell_article_textview, parent, false);
                break;
        }
        return new ArticleLineHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ArticleLineHolder holder, int position) {
        int realPosition = position - 1;
        switch (getItemViewType(position)) {
            case OpusParagraph.TYPE_PIC:
            case OpusParagraph.TYPE_DIVIDER:
                ImageFilterView imageView = (ImageFilterView) holder.itemView;  //图片

                String[] urls = (String[]) paragraphs[realPosition].content;
                // 为什么有时候图片会是空的
                if (urls.length > 1) {
                    Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(urls[0])).placeholder(R.mipmap.placeholder)
                            .transition(GlideUtil.getTransitionOptions())
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(imageView);

                    imageView.setOnClickListener(view -> {
                        Intent intent = new Intent();
                        intent.setClass(context, ImageViewerActivity.class);
                        intent.putExtra("imageList", new ArrayList<>(Arrays.asList(urls)));
                        context.startActivity(intent);
                    });
                }
                break;

            case -1:
                TextView title = holder.itemView.findViewById(R.id.text_title);
                ImageView cover = holder.itemView.findViewById(R.id.img_cover);
                ImageView upIcon = holder.itemView.findViewById(R.id.upInfo_Icon);  //头
                TextView upName = holder.itemView.findViewById(R.id.upInfo_Name);
                MaterialCardView upCard = holder.itemView.findViewById(R.id.upInfo);

                StringUtil.setCopy(title);

                upName.setText(article.upInfo.name);
                if (article.cover.isEmpty()) cover.setVisibility(View.GONE);
                else {
                    Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(article.cover)).placeholder(R.mipmap.placeholder)
                            .transition(GlideUtil.getTransitionOptions())
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(4))))
                            .format(DecodeFormat.PREFER_RGB_565)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(cover);
                }
                Glide.with(BiliTerminal.context).asDrawable().load(GlideUtil.url(article.upInfo.avatar)).placeholder(R.mipmap.akari)
                        .transition(GlideUtil.getTransitionOptions())
                        .apply(RequestOptions.circleCropTransform())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(upIcon);
                upCard.setOnClickListener(view1 -> {
                    Intent intent = new Intent();
                    intent.setClass(context, UserInfoActivity.class);
                    intent.putExtra("mid", article.upInfo.mid);
                    context.startActivity(intent);
                });

                if (article.type == Opus.TYPE_DYNAMIC){
                    holder.itemView.findViewById(R.id.like_coin_fav).setVisibility(View.GONE);
                }

                ImageButton like = holder.itemView.findViewById(R.id.btn_like);
                ImageButton coin = holder.itemView.findViewById(R.id.btn_coin);
                TextView likeLabel = holder.itemView.findViewById(R.id.like_label);
                TextView coinLabel = holder.itemView.findViewById(R.id.coin_label);
                TextView favLabel = holder.itemView.findViewById(R.id.fav_label);
                ImageButton fav = holder.itemView.findViewById(R.id.btn_fav);

                likeLabel.setText(StringUtil.toWan(article.stats.like));
                coinLabel.setText(StringUtil.toWan(article.stats.coin));
                favLabel.setText(StringUtil.toWan(article.stats.favorite));

                like.setOnClickListener(view1 -> CenterThreadPool.run(() -> {
                    try {
                        if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
                            context.runOnUiThread(() -> MsgUtil.showMsg("还没有登录喵~"));
                            return;
                        }
                        int result = ArticleApi.like(article.id, !article.stats.liked);
                        if (result == 0) {
                            article.stats.liked = !article.stats.liked;
                            context.runOnUiThread(() -> {
                                MsgUtil.showMsg((article.stats.liked ? "点赞成功" : "取消成功"));

                                if (article.stats.liked)
                                    likeLabel.setText(StringUtil.toWan(++article.stats.like));
                                else likeLabel.setText(StringUtil.toWan(--article.stats.like));
                                like.setImageResource(article.stats.liked ? R.drawable.icon_like_1 : R.drawable.icon_like_0);
                            });
                        } else {
                            context.runOnUiThread(() -> MsgUtil.showMsg("操作失败：" + result));
                        }
                    } catch (Exception e) {
                        context.runOnUiThread(() -> MsgUtil.err(e));
                    }
                }));

                coin.setOnClickListener(view1 -> CenterThreadPool.run(() -> {
                    if (article.stats.coined < article.stats.coin_limit) {
                        try {
                            if (SharedPreferencesUtil.getLong(SharedPreferencesUtil.mid, 0) == 0) {
                                context.runOnUiThread(() -> MsgUtil.showMsg("还没有登录喵~"));
                                return;
                            }
                            int result = ArticleApi.addCoin(article.id, article.upInfo.mid, 1);
                            if (result == 0) {
                                if(++coinAdd <= 2) article.stats.coined++;
                                context.runOnUiThread(() -> {
                                    MsgUtil.showMsg("投币成功！");
                                    coinLabel.setText(StringUtil.toWan(++article.stats.coin));
                                    coin.setImageResource(R.drawable.icon_coin_1);
                                });
                            } else {
                                String msg = "投币失败：" + result;
                                if (result == 34002) {
                                    msg = "不能给自己投币哦！";
                                }
                                String finalMsg = msg;
                                context.runOnUiThread(() -> MsgUtil.showMsg(finalMsg));
                            }
                        } catch (Exception e) {
                            context.runOnUiThread(() -> MsgUtil.err(e));
                        }
                    } else {
                        context.runOnUiThread(() -> MsgUtil.showMsg("投币数量到达上限"));
                    }
                }));

                fav.setOnClickListener(view1 -> CenterThreadPool.run(() -> {
                    try {
                        if (article.stats.favoured) {
                            if (ArticleApi.delFavorite(article.id) == 0) {
                                context.runOnUiThread(() -> fav.setImageResource(R.drawable.icon_fav_0));
                                article.stats.favorite--;
                            }
                        } else {
                            if (ArticleApi.favorite(article.id) == 0) {
                                context.runOnUiThread(() -> fav.setImageResource(R.drawable.icon_fav_1));
                                article.stats.favorite++;
                            }
                        }
                        article.stats.favoured = !article.stats.favoured;
                        context.runOnUiThread(() -> {
                            favLabel.setText(StringUtil.toWan(article.stats.favorite));
                            MsgUtil.showMsg("操作成功~");
                        });
                    } catch (Exception e) {
                        context.runOnUiThread(() -> MsgUtil.err(e));
                    }
                }));

                title.setText(article.title);
                break;

            case -2:
                TextView views = holder.itemView.findViewById(R.id.viewsCount);
                TextView timeText = holder.itemView.findViewById(R.id.timeText);
                TextView cvidText = holder.itemView.findViewById(R.id.cvidText);
                cvidText.setText("cv" + article.id/* + " | " + article.wordCount + "字"*/);
                StringUtil.setCopy(cvidText, "cv" + article.id);
                views.setText(StringUtil.toWan(article.stats.view) + "阅读");
                timeText.setText(article.pubTime);

                if (article.type == Opus.TYPE_DYNAMIC){
                    holder.itemView.findViewById(R.id.viewsIcon).setVisibility(View.GONE);
                    holder.itemView.findViewById(R.id.viewsCount).setVisibility(View.GONE);
                }
                break;
            case OpusParagraph.TYPE_TEXT:
            case OpusParagraph.TYPE_TEXT_REGULAR:
            default:
                TextView textView = holder.itemView.findViewById(R.id.textView);  //文本
                textView.setText((CharSequence) paragraphs[realPosition].content);
                StringUtil.setCopy(textView);
                StringUtil.setLink(textView);
                break;

        }
    }

    @Override
    public int getItemCount() {
        return paragraphs.length + 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return -1;
        else if (position == paragraphs.length + 1) return -2;
        else return paragraphs[position - 1].type;
    }

    public static class ArticleLineHolder extends RecyclerView.ViewHolder {
        public ArticleLineHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
