package org.vitrivr.cineast.explorative;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by silvanstich on 13.09.16.
 */
public class HCTCell<T> implements IHCTCell {

    private static Logger logger = LogManager.getLogger();
    private final Function<SimpleWeightedGraph<MSTNode<T>, DefaultWeightedEdge>, Double> compactnessFunction;

    private Function<List<List<T>>, Double> distanceCalculation;
    private IMST<T> mst;
    private HCTCell<T> parent;
    private List<HCTCell<T>> children = new ArrayList<>();
    private Function<List<List<T>>, Double> comperatorFunction;

    public HCTCell(Function<SimpleWeightedGraph<MSTNode<T>, DefaultWeightedEdge>, Double> compactnessFunction, Function<List<List<T>>, Double> distanceCalculation, Function<List<List<T>>, Double> comperatorFunction) {
        this.compactnessFunction = compactnessFunction;
        this.distanceCalculation = distanceCalculation;
        this.comperatorFunction = comperatorFunction;
        mst = new MST<>(this.distanceCalculation, comperatorFunction, this.compactnessFunction);
    }

    public HCTCell(Function<SimpleWeightedGraph<MSTNode<T>, DefaultWeightedEdge>, Double> compactnessFunction, Function<List<List<T>>, Double> distanceCalculation, IMST<T> mst, HCTCell<T> parent, Function<List<List<T>>, Double> comperatorFunction) {
        this.compactnessFunction = compactnessFunction;
        this.distanceCalculation = distanceCalculation;
        this.comperatorFunction = comperatorFunction;
        this.mst = new MST<>(this.distanceCalculation, comperatorFunction, this.compactnessFunction);
        this.mst = mst;
        this.parent = parent;
    }

    public void addValue(List<T> value){
        mst.add(value);
    }

    public void removeValue(List<T> value){
        mst.remove(value);
    }

    public double getDistanceToNucleus(List<T> other) throws Exception{
        return mst.getNucleus().distance(other, distanceCalculation);
    }

    public double getCoveringRadius() throws Exception {
        return mst.getCoveringRadius();
    }

    public HCTCell<T> getParent() {
        return parent;
    }

    public void setParent(HCTCell<T> parent) {
        logger.debug("New parent is set. Parent: " + parent + " this: " + this);
        this.parent = parent;
    }

    public List<HCTCell<T>> getChildren(){
        return children;
    }

    public boolean isReadyForMitosis() { return mst.isReadyForMitosis(); }

    public List<HCTCell<T>> mitosis() throws Exception {
        List<MST<T>> msts = mst.mitosis();
        List<HCTCell<T>> newCells = new ArrayList<>();
        for (MST<T> mst : msts) {
            HCTCell<T> newCell = new HCTCell<T>(compactnessFunction, distanceCalculation, mst, parent, comperatorFunction);
            newCells.add(newCell);
            if(parent != null) parent.addChild(newCell);

        }
        for (HCTCell<T> child : children) {
            for (HCTCell<T> newCell : newCells) {
                if(newCell.getValues().contains(child.getNucleus().getValue())){
                    newCell.addChild(child);
                    child.setParent(newCell);
                    break;
                }
            }
        }
        return newCells;

    }

    public MSTNode<T> getNucleus() throws Exception{ return mst.getNucleus(); }

    @Override
    public void addChild(HCTCell child) {
        if(!children.contains(child)) {
            logger.debug("New child is added. Child: " + child + " this: " + this);
            children.add(child);
        } else {
            logger.debug("Child is already in child list. Child: " + child + "this: " + this);
        }
    }

    @Override
    public boolean containsValue(List value) {
        return mst.containsValue(value);
    }

    @Override
    public boolean isCellDeath() {
        return mst.isCellDeath();
    }

    @Override
    public void removeChild(HCTCell child) {
        logger.debug("Child is removed: " + child);
        children.remove(child);
    }

    public String toString(){
        try {
            return String.format("HCTCell | isCellDeath: %s | isReadyMitosis: %s | Nucleus: <%s>",
                    isCellDeath(), isReadyForMitosis(), getNucleus());
        } catch (Exception e){
            return String.format("HCTCell | isCellDeath: %s | isReadyMitosis: %s | Nucleus: <%s>",
                    isCellDeath(), isReadyForMitosis(), "###Error while getting the nucleus! " + e.getMessage());
        }

    }

    public List<List<T>> getValues() {
        return mst.getValues();
    }

    public HCTCell<T> getChildByContainingValue(List<T> value){
        for(HCTCell<T> childCell : children){
            if(childCell.getValues().contains(value)) return childCell;
        }
        return null;
    }
}
