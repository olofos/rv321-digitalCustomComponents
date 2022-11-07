package de.neemann.digital.plugin;

import de.neemann.digital.draw.library.ComponentManager;
import de.neemann.digital.draw.library.ComponentSource;
import de.neemann.digital.draw.library.ElementLibrary;
import de.neemann.digital.draw.library.InvalidNodeException;
import de.neemann.digital.draw.shapes.GenericShape;
import de.neemann.digital.gui.Main;

public class RV321ComponentSource implements ComponentSource {
        @Override
        public void registerComponents(ComponentManager manager) throws InvalidNodeException {

                manager.addComponent("RV321/IO", HD44780LCD.DESCRIPTION);

                manager.addComponent("RV321/Custom", RegViewer.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("RegViewer", inputs, outputs,
                                                attr.getLabel(),
                                                true, 8));

                manager.addComponent("RV321/Custom", RegViewerMonitor.DESCRIPTION,
                                RegViewerMonitorShape::new);

                manager.addComponent("RV321/IO", SdCard.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("SdCard", inputs, outputs, attr.getLabel(),
                                                true, 6));

                manager.addComponent("RV321/74 Series Logic", IC74595.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("74595", inputs, outputs, attr.getLabel(),
                                                true, 6));

                manager.addComponent("RV321/74 Series Logic", IC74597.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("74597", inputs, outputs, attr.getLabel(),
                                                true, 5));

                manager.addComponent("RV321/74 Series Logic", IC744017.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("744017", inputs, outputs, attr.getLabel(),
                                                true, 6));

                manager.addComponent("RV321/74 Series Logic", IC744024.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("744024", inputs, outputs, attr.getLabel(),
                                                true, 4));

                manager.addComponent("RV321/74 Series Logic", IC74161.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("74161", inputs, outputs, attr.getLabel(),
                                                true, 5));

                manager.addComponent("RV321/74 Series Logic", ICHEF4557B.DESCRIPTION,
                                (attr, inputs, outputs) -> new GenericShape("HEF4557B", inputs, outputs,
                                                attr.getLabel(), true, 3));
        }

        /**
         * Start Digital with this ComponentSource attached to make debugging easier.
         * IMPORTANT: Remove the jar from Digitals settings!!!
         *
         * @param args args
         */
        public static void main(String[] args) {
                new Main.MainBuilder()
                                .setLibrary(new ElementLibrary().registerComponentSource(new RV321ComponentSource()))
                                .openLater();
        }
}
