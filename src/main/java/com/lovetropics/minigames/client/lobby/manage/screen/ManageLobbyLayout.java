package com.lovetropics.minigames.client.lobby.manage.screen;

import com.lovetropics.minigames.client.screen.flex.Align;
import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.client.screen.flex.Flex;
import com.lovetropics.minigames.client.screen.flex.FlexSolver;
import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.screens.Screen;

final class ManageLobbyLayout {
	static final int PADDING = 8;
	static final int FOOTER_HEIGHT = 20;

	final Layout header;

	final Layout leftColumn;
	final Layout leftFooter;

	final Layout gameList;

	final Layout centerColumn;
	final Layout centerFooter;

	final Layout centerHeader;
	final Layout edit;

	final Layout play;
	final Layout skip;

	final Layout restart;

	final Layout rightColumn;
	final Layout rightFooter;

	final Layout properties;
	final Layout name;
	final Layout publish;
	final Layout playerList;

	final Layout close;
	final Layout done;

	final Layout[] marginals;

	ManageLobbyLayout(Screen screen) {
		int fontHeight = screen.getMinecraft().font.lineHeight;

		Flex root = new Flex().column();

		Flex header = root.child().row()
				.width(1.0F, Flex.Unit.PERCENT).height(fontHeight).padding(PADDING)
				.alignMain(Align.Main.START);

		Flex body = root.child().row()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F);

		Flex leftColumn = body.child()
				.size(0.25F, 1.0F, Flex.Unit.PERCENT)
				.alignMain(Align.Main.START);

		Flex gameList = leftColumn.child()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F)
				.alignMain(Align.Main.START);

		Flex leftFooter = leftColumn.child()
				.width(1.0F, Flex.Unit.PERCENT).height(FOOTER_HEIGHT).padding(PADDING)
				.alignMain(Align.Main.END);

		Flex centerColumn = body.child().column()
				.height(1.0F, Flex.Unit.PERCENT).grow(1.0F);

		Flex centerHeader = centerColumn.child()
				.width(1.0F, Flex.Unit.PERCENT).height(fontHeight).padding(3)
				.alignMain(Align.Main.START);

		Flex edit = centerColumn.child()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F);

		Flex centerFooter = centerColumn.child().column()
				.width(1.0F, Flex.Unit.PERCENT).height(FOOTER_HEIGHT).padding(PADDING)
				.alignMain(Align.Main.END);

		Flex controls = centerFooter.child().row()
				.alignCross(Align.Cross.CENTER);

		Flex play = controls.child().size(20, 20).margin(2, 0);
		Flex stop = controls.child().size(20, 20).margin(2, 0);
		Flex restart = controls.child().size(20, 20).margin(2, 0);

		Flex rightColumn = body.child().column()
				.size(0.25F, 1.0F, Flex.Unit.PERCENT)
				.alignMain(Align.Main.END);

		Flex properties = rightColumn.child().column()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F).padding(PADDING);

		Flex name = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.margin(2).marginTop(fontHeight);

		Flex publish = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING);

		Flex playerList = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F)
				.marginTop(PADDING);

		Flex rightFooter = rightColumn.child().row()
				.width(1.0F, Flex.Unit.PERCENT)
				.padding(PADDING / 2)
				.alignMain(Align.Main.END);

		Flex close = rightFooter.child()
				.grow(1.0F).height(20)
				.margin(PADDING / 2);

		Flex done = rightFooter.child()
				.grow(1.0F).height(20)
				.margin(PADDING / 2);

		FlexSolver.Results solve = new FlexSolver(new Box(screen)).apply(root);

		this.header = solve.layout(header);

		this.leftColumn = solve.layout(leftColumn);
		this.leftFooter = solve.layout(leftFooter);
		this.gameList = solve.layout(gameList);

		this.centerColumn = solve.layout(centerColumn);
		this.centerHeader = solve.layout(centerHeader);
		this.centerFooter = solve.layout(centerFooter);
		this.edit = solve.layout(edit);

		this.play = solve.layout(play);
		skip = solve.layout(stop);
		this.restart = solve.layout(restart);

		this.rightColumn = solve.layout(rightColumn);
		this.rightFooter = solve.layout(rightFooter);
		this.properties = solve.layout(properties);
		this.name = solve.layout(name);
		this.publish = solve.layout(publish);
		this.playerList = solve.layout(playerList);
		this.close = solve.layout(close);
		this.done = solve.layout(done);

		marginals = new Layout[]{this.header, this.leftFooter, this.centerFooter, this.rightFooter};
	}
}
