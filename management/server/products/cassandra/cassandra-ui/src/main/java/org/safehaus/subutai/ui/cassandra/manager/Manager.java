/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.cassandra.manager;


import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import org.safehaus.subutai.api.cassandra.Config;
import org.safehaus.subutai.server.ui.component.ConfirmationDialog;
import org.safehaus.subutai.server.ui.component.ProgressWindow;
import org.safehaus.subutai.server.ui.component.TerminalWindow;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.shared.protocol.Util;
import org.safehaus.subutai.ui.cassandra.CassandraUI;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * @author dilshat
 */
public class Manager {

	private final VerticalLayout contentRoot;
	private final ComboBox clusterCombo;
	private final Table nodesTable;
	private Config config;


	public Manager() {

		contentRoot = new VerticalLayout();
		contentRoot.setSpacing(true);
		contentRoot.setSizeFull();

		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();

		contentRoot.addComponent(content);
		contentRoot.setComponentAlignment(content, Alignment.TOP_CENTER);
		contentRoot.setMargin(true);

		//tables go here
		nodesTable = createTableTemplate("Cluster nodes", 300);

		HorizontalLayout controlsContent = new HorizontalLayout();
		controlsContent.setSpacing(true);

		Label clusterNameLabel = new Label("Select the cluster");
		controlsContent.addComponent(clusterNameLabel);
		controlsContent.setComponentAlignment(clusterNameLabel, Alignment.MIDDLE_CENTER);

		clusterCombo = new ComboBox();
		clusterCombo.setImmediate(true);
		clusterCombo.setTextInputAllowed(false);
		clusterCombo.setWidth(200, Sizeable.Unit.PIXELS);
		clusterCombo.addValueChangeListener(new Property.ValueChangeListener() {
			@Override
			public void valueChange(Property.ValueChangeEvent event) {
				config = (Config) event.getProperty().getValue();
				refreshUI();
			}
		});

		controlsContent.addComponent(clusterCombo);

		Button refreshClustersBtn = new Button("Refresh clusters");
		refreshClustersBtn.addStyleName("default");
		refreshClustersBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				refreshClustersInfo();
			}
		});

		controlsContent.addComponent(refreshClustersBtn);

		Button checkAllBtn = new Button("Check all");
		checkAllBtn.addStyleName("default");
		checkAllBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				UUID trackID = CassandraUI.getCassandraManager().checkAllNodes(config.getClusterName());
				ProgressWindow window = new ProgressWindow(CassandraUI.getExecutor(), CassandraUI.getTracker(), trackID, Config.PRODUCT_KEY);
				window.getWindow().addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(Window.CloseEvent closeEvent) {
						refreshClustersInfo();
					}
				});
				contentRoot.getUI().addWindow(window.getWindow());
			}
		});

		controlsContent.addComponent(checkAllBtn);

		Button startAllBtn = new Button("Start all");
		startAllBtn.addStyleName("default");
		startAllBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				UUID trackID = CassandraUI.getCassandraManager().startAllNodes(config.getClusterName());
				ProgressWindow window = new ProgressWindow(CassandraUI.getExecutor(), CassandraUI.getTracker(), trackID, Config.PRODUCT_KEY);
				window.getWindow().addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(Window.CloseEvent closeEvent) {
						refreshClustersInfo();
					}
				});
				contentRoot.getUI().addWindow(window.getWindow());
			}
		});

		controlsContent.addComponent(startAllBtn);

		Button stopAllBtn = new Button("Stop all");
		stopAllBtn.addStyleName("default");
		stopAllBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				UUID trackID = CassandraUI.getCassandraManager().stopAllNodes(config.getClusterName());
				ProgressWindow window = new ProgressWindow(CassandraUI.getExecutor(), CassandraUI.getTracker(), trackID, Config.PRODUCT_KEY);
				window.getWindow().addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(Window.CloseEvent closeEvent) {
						refreshClustersInfo();
					}
				});
				contentRoot.getUI().addWindow(window.getWindow());
			}
		});

		Button destroyClusterBtn = new Button("Destroy cluster");
		destroyClusterBtn.addStyleName("default");
		destroyClusterBtn.addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(Button.ClickEvent clickEvent) {
				if (config != null) {
					ConfirmationDialog alert = new ConfirmationDialog(String.format("Do you want to destroy the %s cluster?", config.getClusterName()),
							"Yes", "No");
					alert.getOk().addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(Button.ClickEvent clickEvent) {
							UUID trackID = CassandraUI.getCassandraManager()
									.uninstallCluster(config.getClusterName());

							ProgressWindow window = new ProgressWindow(CassandraUI.getExecutor(), CassandraUI.getTracker(), trackID, Config.PRODUCT_KEY);
							window.getWindow().addCloseListener(new Window.CloseListener() {
								@Override
								public void windowClose(Window.CloseEvent closeEvent) {
									refreshClustersInfo();
								}
							});
							contentRoot.getUI().addWindow(window.getWindow());
						}
					});

					contentRoot.getUI().addWindow(alert.getAlert());
				} else {
					show("Please, select cluster");
				}
			}
		});

		controlsContent.addComponent(stopAllBtn);
		controlsContent.addComponent(destroyClusterBtn);
		content.addComponent(controlsContent);
		content.addComponent(nodesTable);

	}

	private Table createTableTemplate(String caption, int size) {
		final Table table = new Table(caption);
		table.addContainerProperty("Host", String.class, null);
		table.addContainerProperty("Status", Embedded.class, null);
		table.setWidth(100, Sizeable.Unit.PERCENTAGE);
		table.setHeight(size, Sizeable.Unit.PIXELS);
		table.setPageLength(10);
		table.setSelectable(false);
		table.setImmediate(true);
		table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
			@Override
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick()) {
					String lxcHostname = (String) table.getItem(event.getItemId()).getItemProperty("Host").getValue();
					Agent lxcAgent = CassandraUI.getAgentManager().getAgentByHostname(lxcHostname);
					if (lxcAgent != null) {
						TerminalWindow terminal = new TerminalWindow(Util.wrapAgentToSet(lxcAgent), CassandraUI.getExecutor(), CassandraUI.getCommandRunner(), CassandraUI.getAgentManager());
						contentRoot.getUI().addWindow(terminal.getWindow());
					} else {
						show("Agent is not connected");
					}
				}
			}
		});
		return table;
	}

	private void refreshUI() {
		if (config != null) {
			populateTable(nodesTable, config.getNodes());
		} else {
			nodesTable.removeAllItems();
		}
	}

	public void refreshClustersInfo() {
		List<Config> info = CassandraUI.getCassandraManager().getClusters();
		Config clusterInfo = (Config) clusterCombo.getValue();
		clusterCombo.removeAllItems();
		if (info != null && info.size() > 0) {
			for (Config mongoInfo : info) {
				clusterCombo.addItem(mongoInfo);
				clusterCombo.setItemCaption(mongoInfo,
						mongoInfo.getClusterName());
			}
			if (clusterInfo != null) {
				for (Config cassandraInfo : info) {
					if (cassandraInfo.getClusterName().equals(clusterInfo.getClusterName())) {
						clusterCombo.setValue(cassandraInfo);
						return;
					}
				}
			} else {
				clusterCombo.setValue(info.iterator().next());
			}
		}
	}

	private void show(String notification) {
		Notification.show(notification);
	}

	private void populateTable(final Table table, Set<Agent> agents) {
		table.removeAllItems();
		for (Iterator it = agents.iterator(); it.hasNext(); ) {
			final Agent agent = (Agent) it.next();
			final Embedded progressIcon = new Embedded("",
					new ThemeResource("img/spinner.gif"));
			progressIcon.setVisible(false);
			final Object rowId = table.addItem(new Object[] {
							agent.getHostname(),
							progressIcon},
					null
			);
		}
	}

	public Component getContent() {
		return contentRoot;
	}

}
