package org.lovetropics.games.lobbies.client.manage.screen;

import net.minecraft.client.gui.screens.Screen;
import org.lovetropics.games.lobbies.client.screen.flex.Align;
import org.lovetropics.games.lobbies.client.screen.flex.Box;
import org.lovetropics.games.lobbies.client.screen.flex.Flex;
import org.lovetropics.games.lobbies.client.screen.flex.FlexSolver;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;

final class ManageLobbyLayout {
	static final int PADDING = 8;
	static final int FOOTER_HEIGHT = 20;

	final Layout header;
	final Layout footer;

	final Layout leftColumn;
	final Layout leftFooter;

	final Layout gameList;

	final Layout rightColumn;

	final Layout properties;
	final Layout name;
	final Layout close;
	final Layout focusLive;
	final Layout separator;

	final Layout playersLock;
	final Layout playersTitle;
	final Layout playersAdd;
	final Layout playerList;

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

		Flex footer = root.child().row()
				.width(1.0F, Flex.Unit.PERCENT).height(FOOTER_HEIGHT).padding(PADDING)
				.alignMain(Align.Main.END);

		Flex leftFooter = footer.child().row()
				.height(1.0F, Flex.Unit.PERCENT).grow(1.0f)
				.alignCross(Align.Cross.START);

		Flex rightFooter = footer.child().row()
				.height(1.0F, Flex.Unit.PERCENT).grow(1.0f)
				.alignCross(Align.Cross.END);

		Flex leftColumn = body.child()
				.height(1.0f, Flex.Unit.PERCENT).grow(1.0f)
				.alignMain(Align.Main.START);

		Flex gameList = leftColumn.child()
				.size(1.0f, 1.0f, Flex.Unit.PERCENT)
				.alignMain(Align.Main.START);

		Flex rightColumn = body.child().column()
				.size(0.4f, 1.0F, Flex.Unit.PERCENT)
				.alignMain(Align.Main.END);

		Flex properties = rightColumn.child().column()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F).padding(PADDING);

		Flex name = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.margin(2).marginTop(fontHeight);

		Flex close = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING);

		Flex focusLive = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING / 2);

		Flex separator = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).height(1)
				.marginTop(PADDING);

		Flex playersHeader = properties.child().row()
				.width(1.0F, Flex.Unit.PERCENT).height(20)
				.marginTop(PADDING)
				.alignCross(Align.Cross.CENTER);

		Flex playersLock = playersHeader.child().size(20, 20);
		Flex playersTitle = playersHeader.child().height(20).grow(1.0F);
		Flex playersAdd = playersHeader.child().size(20, 20);

		Flex playerList = properties.child()
				.width(1.0F, Flex.Unit.PERCENT).grow(1.0F)
				.marginTop(PADDING / 2);

		Flex done = rightFooter.child()
				.width(0.5F, Flex.Unit.PERCENT).height(20)
				.margin(2, 0)
				.alignMain(Align.Main.END);

		FlexSolver.Results solve = new FlexSolver(new Box(screen)).apply(root);

		this.header = solve.layout(header);
		this.footer = solve.layout(footer);

		this.leftColumn = solve.layout(leftColumn);
		this.leftFooter = solve.layout(leftFooter);
		this.gameList = solve.layout(gameList);

		this.rightColumn = solve.layout(rightColumn);
		this.properties = solve.layout(properties);
		this.name = solve.layout(name);
		this.close = solve.layout(close);
		this.focusLive = solve.layout(focusLive);
		this.separator = solve.layout(separator);
		this.playersLock = solve.layout(playersLock);
		this.playersTitle = solve.layout(playersTitle);
		this.playersAdd = solve.layout(playersAdd);
		this.playerList = solve.layout(playerList);
		this.done = solve.layout(done);

		marginals = new Layout[]{this.header, this.footer};
	}
}
