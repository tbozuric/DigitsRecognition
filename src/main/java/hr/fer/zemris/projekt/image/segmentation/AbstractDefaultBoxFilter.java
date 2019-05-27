package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.filters.Filter;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public abstract class AbstractDefaultBoxFilter implements BoundingBoxFinder {

    private static final int MAXIMUM_DISTANCE_BETWEEN_BOXES = 4;
    private static final int MAXIMUM_DISTANCE_VERTICALLY = 4;
    private static final double MINIMUM_WIDTH_RATIO_BETWEEN_BOXES = 1.4;
    private static final double MAXIMUM_AVERAGE_HEIGHT_FACTOR = 2.0;
    private static final double MAXIMUM_HEIGHT_RATIO = 0.62;//0.62
    private static final int MINIMUM_DISTANCE_BETWEEN_Y_AXES_OF_BOXES = 6;

    //private static final int MINIMUM_BOX_HEIGHT_WIDTH = 5;

    @Override
    public List<BoundingBox> filter(List<BoundingBox> boxes, List<Filter<BoundingBox>> filters) {


        int size = boxes.size();

        int totalHeight = 0;

        for (BoundingBox box : boxes) {
            totalHeight += box.getHeight();
        }

        float averageHeight = totalHeight / (float) boxes.size();

        Map<BoundingBox, Set<BoundingBox>> itemsForMerging = new HashMap<>();
        Map<Pair<BoundingBox, BoundingBox>, Double> cacheDistance = new HashMap<>();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    BoundingBox b1 = boxes.get(i);
                    BoundingBox b2 = boxes.get(j);

                    double distance;

                    Pair<BoundingBox, BoundingBox> pairFirst = Pair.create(b1, b2);
                    Pair<BoundingBox, BoundingBox> pairSecond = Pair.create(b2, b1);


                    if (cacheDistance.get(pairFirst) == null && cacheDistance.get(pairSecond) == null) {
                        distance = b1.minimumDistanceToBox(b2);
                        cacheDistance.put(pairFirst, distance);
                        cacheDistance.put(pairSecond, distance);
                    } else {
                        distance = cacheDistance.get(pairFirst);
                    }

                    BoundingBox outerBox = BoundingBox.getOuterBox(b1, b2);
                    if (distance <= MAXIMUM_DISTANCE_BETWEEN_BOXES) {

                        int widthOfFirstBox = b1.getWidth();
                        int widthOfSecondBox = b2.getWidth();

                        double widthRatio = getRatio(widthOfFirstBox, widthOfSecondBox,
                                widthOfFirstBox > widthOfSecondBox);

                        Point p1 = b1.getUpLeft();
                        Point p2 = b2.getUpLeft();
                        int heightOfFirstBox = b1.getHeight();
                        int heightOfSecondBox = b2.getHeight();


                        double heightRatio = getRatio(heightOfFirstBox, heightOfSecondBox,
                                heightOfFirstBox > heightOfSecondBox);

                        int yDownFirst;
                        int yDownSecond;



                        if (p1.getY() < p2.getY()) {
                            yDownFirst = p1.getY() + b1.getHeight();
                            yDownSecond = p2.getY();
                        } else {
                            yDownFirst = p2.getY() + b2.getHeight();
                            yDownSecond = p1.getY();
                        }

                        if ((1.0 / heightRatio) >= MAXIMUM_HEIGHT_RATIO &&
                                Math.abs(yDownFirst - yDownSecond) > MAXIMUM_DISTANCE_VERTICALLY
                                && Math.abs(p1.getY() - p2.getY()) <= MINIMUM_DISTANCE_BETWEEN_Y_AXES_OF_BOXES)   {
                            continue;
                        }


//                        if(heightOfFirstBox <= MINIMUM_BOX_HEIGHT_WIDTH && widthOfFirstBox <=MINIMUM_BOX_HEIGHT_WIDTH){
//                            continue;
//                        }
//
//                        if(heightOfSecondBox <= MINIMUM_BOX_HEIGHT_WIDTH && widthOfSecondBox <=MINIMUM_BOX_HEIGHT_WIDTH){
//                            continue;
//                        }

                        //if the wider box is 60% wider than the other then that digits, it may need to be separated
                        if (widthRatio <= MINIMUM_WIDTH_RATIO_BETWEEN_BOXES) {
                            int x1;
                            int x2;
                            int width;

                            if (p1.getX() < p2.getX()) {
                                x1 = p1.getX();
                                width = widthOfFirstBox;
                                x2 = p2.getX();
                            } else {
                                x1 = p2.getX();
                                width = widthOfSecondBox;
                                x2 = p1.getX();
                            }

                            //if the box with smaller height is at least 80% high as taller box than that digits
                            // should be separated by the boxes
                            if (heightRatio > MAXIMUM_HEIGHT_RATIO) {
                                if (!(x2 < x1 + width)) {
                                    continue;
                                }
                            }
                        }

                        if (outerBox.getHeight() > averageHeight * MAXIMUM_AVERAGE_HEIGHT_FACTOR) {
                            continue;
                        }

                        itemsForMerging.computeIfAbsent(b1, k -> new HashSet<>());
                        itemsForMerging.get(b1).add(b2);
                    }
                }
            }
        }

        Map<BoundingBox, Set<BoundingBox>> chainFilteredBoxes = new HashMap<>();
        Set<BoundingBox> visitedBoxes = new HashSet<>();

        //this part resolving the problem with chained boxes, for example if b1 needs to be merged with b2, b3,
        // b4 and b2 needs to be merged with b6 then the final result will be b1 -> b2,b3,b4,b6
        for (Map.Entry<BoundingBox, Set<BoundingBox>> entry : itemsForMerging.entrySet()) {
            if (visitedBoxes.contains(entry.getKey())) {
                continue;
            }
            Set<BoundingBox> boxesForProcessing = entry.getValue();
            Set<BoundingBox> finalBoxesForMerging = new HashSet<>();

            while (boxesForProcessing.size() != 0) {
                Set<BoundingBox> child = new HashSet<>();
                for (BoundingBox b : boxesForProcessing) {
                    finalBoxesForMerging.add(b);
                    if (itemsForMerging.get(b) != null) {
                        child.addAll(itemsForMerging.get(b));
                    }
                }
                boxesForProcessing = new HashSet<>();
                for (BoundingBox children : child) {
                    if (!finalBoxesForMerging.contains(children)) {
                        boxesForProcessing.add(children);
                    }
                }
            }

            visitedBoxes.add(entry.getKey());
            visitedBoxes.addAll(finalBoxesForMerging);

            chainFilteredBoxes.put(entry.getKey(), finalBoxesForMerging);
        }

        Set<BoundingBox> boxesForRemoving = new HashSet<>();
        Set<BoundingBox> newBoxes = new HashSet<>();
        for (Map.Entry<BoundingBox, Set<BoundingBox>> entry : chainFilteredBoxes.entrySet()) {
            BoundingBox b = entry.getKey();
            Set<BoundingBox> boxesForMerging = entry.getValue();

            BoundingBox finalBox = b;
            for (BoundingBox box : boxesForMerging) {
                finalBox = BoundingBox.getOuterBox(finalBox, box);
            }

            boxesForRemoving.add(b);
            boxesForRemoving.addAll(boxesForMerging);

            newBoxes.add(finalBox);
        }

        Set<BoundingBox> results = new HashSet<>(boxes);
        results.removeAll(boxesForRemoving);
        results.addAll(newBoxes);


        //final filters received through the parameter
        List<BoundingBox> finalResults = new ArrayList<>();
        for (BoundingBox box : results) {
            boolean isOk = true;
            for (Filter<BoundingBox> filter : filters) {
                if (!filter.getFilter().test(box)) {
                    isOk = false;
                    break;
                }
            }
            if (isOk) {
                finalResults.add(box);
            }
        }


        return finalResults;
    }

    private double getRatio(int firstValue, int secondValue, boolean isBiggerFirstValue) {
        double widthRatio;

        if (isBiggerFirstValue) {
            widthRatio = firstValue / (double) secondValue;
        } else {
            widthRatio = secondValue / (double) firstValue;
        }
        return widthRatio;
    }

}
