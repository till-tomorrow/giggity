/*
 * Giggity -- Android app to view conference/festival schedules
 * Copyright 2008-2011 Wilmer van der Gaast <wilmer@gaast.net>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package net.gaast.giggity;

import java.util.Date;
import java.util.LinkedList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

public class EventDialog extends Dialog implements OnDismissListener {
	private Context ctx;
	private Schedule.Item item;
	private OnDismissListener dismissPassThru;

	private CheckBox cb;
	private StarsView sv;

	public EventDialog(Context ctx_, Schedule.Item item_) {
		super(ctx_);

		ctx = ctx_;
		item = item_;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/* Rendering our own title (ScheduleItemView) */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(genDialog(false));
		
		super.setOnDismissListener(this);

		super.onCreate(savedInstanceState);
	}
	
	public LinearLayout genDialog(boolean big) {
		Giggity app = (Giggity) ctx.getApplicationContext();
		String descs = "";
		if (item.getSpeakers() != null) {
			if (item.getSpeakers().size() > 1)
				descs += ctx.getResources().getString(R.string.speakers) + " ";
			else
				descs += ctx.getResources().getString(R.string.speaker) + " ";
			for (String i : item.getSpeakers())
				descs += i + ", ";
			descs = descs.replaceAll(", $", "\n\n");
		}
		if (item.getDescription() != null)
			descs += item.getDescription();
		
		LinearLayout content = new LinearLayout(ctx);
		content.setOrientation(LinearLayout.VERTICAL);

		/* Title, styled like in ScheduleListView. */
		View title = new ScheduleItemView(ctx, item, 0); //ScheduleItemView.SHORT_TITLE);
		content.addView(title, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));
		
		/* Separator line between dialog title and rest. */
		ImageView line = new ImageView(ctx);
		line.setImageDrawable(ctx.getResources().getDrawable(android.R.drawable.divider_horizontal_dark));
		line.setScaleType(ImageView.ScaleType.FIT_XY);
		content.addView(line, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));
		
		/* Big text field with abstract, description, etc. */
		TextView desc = new TextView(ctx);
		desc.setText(descs);
		ScrollView descscr = new ScrollView(ctx);
		if (!item.getSchedule().hasLinkTypes() && item.getLinks() != null) {
			LinearLayout descLinks = new LinearLayout(ctx);
			descLinks.setOrientation(LinearLayout.VERTICAL);
			descLinks.addView(desc);
			
			for (Schedule.Item.Link link : item.getLinks()) {
				LinkButton btn = new LinkButton(ctx, link);
				btn.showUrl(true);
				descLinks.addView(btn, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
			}
			descscr.addView(descLinks);
			app.setPadding(desc, 0, 0, 0, 16);
		} else
			descscr.addView(desc);
		
		content.addView(descscr, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));

		/* Bottom box is some stuff next to each other: Remind checkbox/stars, web buttons. */
		LinearLayout bottomBox = new LinearLayout(ctx);
		bottomBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

		if (item.getStartTime().after(new Date())) {
			/* Show "Remind me" only if the event is in the future. */
			cb = new CheckBox(ctx);
			cb.setText(R.string.remind_me);
			cb.setChecked(item.getRemind());
			bottomBox.addView(cb, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		} else {
			/* Otherwise, stars to rate the event. Using my own StarsView because the stock one's too huge. */
			sv = new StarsView(ctx);
			sv.setNumStars(5);
			sv.setScore(item.getStars());
			/* Bigger surface for easier touching. */
			sv.setMinimumHeight(48);
			bottomBox.addView(sv, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
		}
		
		/* Web buttons on the right, if we have types. */
		if (item.getSchedule().hasLinkTypes()) {
			LinkedList<Schedule.Item.Link> links = item.getLinks();
			if (links != null) {
				LinearLayout webButtons = new LinearLayout(ctx);
				for (Schedule.Item.Link link : links) {
					LinkButton btn = new LinkButton(ctx, link);
					webButtons.addView(btn);
				}
				bottomBox.addView(webButtons, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
			}
		}

		content.addView(bottomBox, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0));
		
		if (big) {
			app.setPadding(title, 8 , 8, 8, 6);
			app.setPadding(descscr, 8, 6, 8, 8);
		}
		
		return content;
	}
	
	private class LinkButton extends FrameLayout implements ImageButton.OnClickListener {
		Schedule.Item.Link link;
		
		public LinkButton(Context ctx, Schedule.Item.Link link_) {
			super(ctx);
			link = link_;
			showUrl(false);
		}
		
		public void showUrl(boolean show) {
			this.removeAllViews();
			if (show) {
				TextView url = new TextView(ctx);
				url.setText("• " + link.getUrl());
				url.setEllipsize(TextUtils.TruncateAt.END);
				url.setOnClickListener(this);
				url.setSingleLine();
				url.setPadding(0, 3, 0, 3);
				addView(url);
			} else {
				ImageButton btn = new ImageButton(ctx);
				btn.setImageDrawable(link.getType().getIcon());
				btn.setOnClickListener(this);
				addView(btn);
			}
		}
		
		@Override
		public void onClick(View v) {
			Uri uri = Uri.parse(link.getUrl());
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			ctx.startActivity(intent);
		}
	}
	
	@Override
	public void setOnDismissListener(OnDismissListener l) {
		/* Multiple listeners are not supported, but this hack solves
		 * that problem for me. */
		dismissPassThru = l;
	}

	public void onDismiss(DialogInterface dialog) {
		if (cb != null) {
			item.setRemind(cb.isChecked());
			/* See if I'll use this.
			if (cb.isChecked()) {
				Intent intent = new Intent(Intent.ACTION_EDIT);
				intent.setType("vnd.android.cursor.item/event");
				intent.putExtra("beginTime", item.getStartTime().getTime());
				intent.putExtra("endTime", item.getEndTime().getTime());
				intent.putExtra("title", item.getTitle());
				intent.putExtra("eventLocation", item.getLine().getTitle());
				intent.putExtra("description", item.getDescription());
				ctx.startActivity(intent);
			}
		*/
		}
		if (sv != null)
			item.setStars(sv.getScore());
		if (dismissPassThru != null)
			dismissPassThru.onDismiss(dialog);
	}

	@Override
	public void show() {
		try {
			ScheduleViewActivity sva = (ScheduleViewActivity) ctx;
			if (sva.setEventDialog(this))
				return;
		} catch (ClassCastException e) {
		}
		super.show();
	}
}
