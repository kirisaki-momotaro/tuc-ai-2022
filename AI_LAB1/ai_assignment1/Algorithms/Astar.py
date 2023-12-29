import copy
import time
import sys
from abc import ABC
from typing import Tuple, Union, Dict, List, Any
import math

import numpy as np

from commonroad.scenario.trajectory import State

sys.path.append('../')
from SMP.maneuver_automaton.motion_primitive import MotionPrimitive
from SMP.motion_planner.node import Node, CostNode
from SMP.motion_planner.plot_config import DefaultPlotConfig
from SMP.motion_planner.queue import FIFOQueue, LIFOQueue
from SMP.motion_planner.search_algorithms.base_class import SearchBaseClass
from SMP.motion_planner.utility import MotionPrimitiveStatus, initial_visualization, update_visualization


class SequentialSearch(SearchBaseClass, ABC):
    """
    Abstract class for search motion planners.
    """

    # declaration of class variables
    path_fig: Union[str, None]
    w=2
    visited_nodes_num = 0

    def __init__(self, scenario, planningProblem, automaton, plot_config=DefaultPlotConfig):
        super().__init__(scenario=scenario, planningProblem=planningProblem, automaton=automaton,
                         plot_config=plot_config)

    def initialize_search(self, time_pause, cost=True):
        """
        initializes the visualizer
        returns the initial node
        """
        self.list_status_nodes = []
        self.dict_node_status: Dict[int, Tuple] = {}
        self.time_pause = time_pause
        self.visited_nodes = []

        # first node
        if cost:
            node_initial = CostNode(list_paths=[[self.state_initial]],
                                    list_primitives=[self.motion_primitive_initial],
                                    depth_tree=0, cost=0)
        else:
            node_initial = Node(list_paths=[[self.state_initial]],
                                list_primitives=[self.motion_primitive_initial],
                                depth_tree=0)
        initial_visualization(self.scenario, self.state_initial, self.shape_ego, self.planningProblem,
                              self.config_plot, self.path_fig)
        self.dict_node_status = update_visualization(primitive=node_initial.list_paths[-1],
                                                     status=MotionPrimitiveStatus.IN_FRONTIER,
                                                     dict_node_status=self.dict_node_status, path_fig=self.path_fig,
                                                     config=self.config_plot,
                                                     count=len(self.list_status_nodes), time_pause=self.time_pause)
        self.list_status_nodes.append(copy.copy(self.dict_node_status))
        return node_initial

    def take_step(self, successor, node_current, cost=True):
        """
        Visualizes the step of a successor and checks if it collides with either an obstacle or a boundary
        cost is equal to the cost function up until this node
        Returns collision boolean and the child node if it does not collide
        """
        # translate and rotate motion primitive to current position
        list_primitives_current = copy.copy(node_current.list_primitives)
        path_translated = self.translate_primitive_to_current_state(successor,
                                                                    node_current.list_paths[-1])
        list_primitives_current.append(successor)
        self.path_new = node_current.list_paths + [[node_current.list_paths[-1][-1]] + path_translated]
        if cost:
            child = CostNode(list_paths=self.path_new,
                             list_primitives=list_primitives_current,
                             depth_tree=node_current.depth_tree + 1,
                             cost=self.cost_function(node_current))
        else:
            child = Node(list_paths=self.path_new, list_primitives=list_primitives_current,
                         depth_tree=node_current.depth_tree + 1)

        # check for collision, skip if is not collision-free
        if not self.is_collision_free(path_translated):
            position = self.path_new[-1][-1].position.tolist()
            self.list_status_nodes, self.dict_node_status, self.visited_nodes = self.plot_colliding_primitives(
                current_node=node_current,
                path_translated=path_translated,
                node_status=self.dict_node_status,
                list_states_nodes=self.list_status_nodes,
                time_pause=self.time_pause,
                visited_nodes=self.visited_nodes)
            return True, child
        self.update_visuals()
        return False, child

    def update_visuals(self):
        """
        Visualizes a step on plot
        """
        position = self.path_new[-1][-1].position.tolist()
        if position not in self.visited_nodes:
            self.dict_node_status = update_visualization(primitive=self.path_new[-1],
                                                         status=MotionPrimitiveStatus.IN_FRONTIER,
                                                         dict_node_status=self.dict_node_status, path_fig=self.path_fig,
                                                         config=self.config_plot,
                                                         count=len(self.list_status_nodes), time_pause=self.time_pause)
            self.list_status_nodes.append(copy.copy(self.dict_node_status))
        self.visited_nodes.append(position)

    def goal_reached(self, successor, node_current):
        """
        Checks if the goal is reached.
        Returns True/False if goal is reached
        """
        path_translated = self.translate_primitive_to_current_state(successor,
                                                                    node_current.list_paths[-1])
        # goal test
        if self.reached_goal(path_translated):
            # goal reached
            self.path_new = node_current.list_paths + [[node_current.list_paths[-1][-1]] + path_translated]
            path_solution = self.remove_states_behind_goal(self.path_new)
            self.list_status_nodes = self.plot_solution(path_solution=path_solution, node_status=self.dict_node_status,
                                                        list_states_nodes=self.list_status_nodes,
                                                        time_pause=self.time_pause)
            return True
        return False

    def get_obstacles_information(self):
        """
        Information regarding the obstacles.
        Returns a list of obstacles' information, each element
        contains information regarding an obstacle:
        [x_center_position, y_center_position, length, width]

        """
        return self.extract_collision_obstacles_information()

    def get_goal_information(self):
        """
        Information regarding the goal.
        Returns a list of the goal's information
        with the following form:
        [x_center_position, y_center_position, length, width]
        """
        return self.extract_goal_information()

    def get_node_information(self, node_current):
        """
        Information regarding the input node_current.
        Returns a list of the node's information
        with the following form:
        [x_center_position, y_center_position]
        """
        return node_current.get_position()

    def get_node_path(self, node_current):
        """
        Information regarding the input node_current.
        Returns the path starting from the initial node and ending at node_current.
        """
        return node_current.get_path()

    def cost_function(self, node_current):
        """
        Returns g(n) from initial to current node, !only works with cost nodes!
        """
        velocity = node_current.list_paths[-1][-1].velocity

        node_center = self.get_node_information(node_current)
        goal_center = self.get_goal_information()
        distance_x = goal_center[0] - node_center[0]
        distance_y = goal_center[1] - node_center[1]
        length_goal = goal_center[2]
        width_goal = goal_center[3]

        distance = 4.5
        if (abs(distance_x) < length_goal / 2 and abs(distance_y) < width_goal / 2):
            prev_x = node_current.list_paths[-2][-1].position[0]
            prev_y = node_current.list_paths[-2][-1].position[1]
            distance = goal_center[0] - length_goal / 2 - prev_x
        cost = node_current.cost + distance

        return cost

    def heuristic_function(self, node_current):
        """
        Enter your heuristic function h(x) calculation of distance from node_current to goal
        Returns the distance normalized to be comparable with cost function measurements
        """
        node_center = self.get_node_information(node_current)
        goal_center = self.get_goal_information()
        distance_x = goal_center[0] - node_center[0]
        distance_y = goal_center[1] - node_center[1]
        length_goal = goal_center[2]
        width_goal = goal_center[3]

        distance = math.sqrt((distance_x ** 2) + (distance_y ** 2))

        return distance

    def heuristic_function_2(self, node_current):
        """
        Non-Euclidean Heuristic. See heuristic_helper for details
        """

        # find the prograde (velocity) angle of the node and the angle between the node and goal
        paths = node_current.list_paths[-1][-1]
        prograde_angle = paths.orientation
        angle_to_goal = self.calc_angle_to_goal(paths)

        # subtract one from the other to get the angle between the prograde and the goal
        # the measure of how "on target" the node is
        off_angle = angle_to_goal - prograde_angle
        euc_distance = self.heuristic_function(node_current)
        return self.heuristic_helper(euc_distance=euc_distance, angle=off_angle)

    def heuristic_helper(self, euc_distance, angle):
        """
        The heart of the Non-Euclidean Heuristic. Calculate distance as an arc + a straight line segment.
        Given correct turning radius, this heuristic is perfect (re; the exact path length)
        for the case of no obstacles (and point target).
        Concessions:
        1) The function does not take obstacles into account. Not that implementing this is too hard,
        but I'm assuming the function is not "supposed" to know about any obstacles from the problem as given
        2) As the angle tends to 0 this distance tends to the euclidean (we're already "locked on")
        3) As the euclidean tends to infinity this distance tends to
        the euclidean + a constant linearly related to the off-angle (the turning part doesn't change the position much)
        """
        # the turning radius, found experimentally
        r = 12.903
        # a safety margin just in case
        # any margin between 0 and 1 only causes underestimation and thus still guarantees the shortest path
        margin = 0.1
        # the radius we'll use
        r = r * (1 - margin)

        # a very big number to return when it's impossible to reach the target in the maneuver
        very_big_number_ohno = 69

        # the euclidean distance
        d = euc_distance

        # the off-angle from the target (radians)
        temp_ang = abs(angle) % (2 * math.pi)
        theta = min(temp_ang, 2 * math.pi - temp_ang)
        # assert (0 <= theta <= math.pi)

        if theta > math.pi / 2:
            # the width of the road we're on
            road_bounds = self.extract_outer_lanelet_bounds()
            road_width = road_bounds[0] - road_bounds[1]
            if r > road_width / 2:
                # We can't turn in time to avoid going off-road!
                return very_big_number_ohno
            # This code is irrelevant in all the given scenaria, but will activate if we're given a wider road
            # the excess angle
            phi = theta - math.pi / 2
            # the pre-arc angle
            omega = math.asin((d * math.cos(phi) - r) / math.sqrt(d ** 2 + r ** 2 - 2 * d * r * math.cos(phi)))
            # the new pivot point
            y_r = r / math.sqrt(1 + math.tan(phi) ** 2)
            x_r = y_r / math.tan(phi)
            d_v = r + math.sqrt((x_r - d) ** 2 + y_r ** 2)
            l_s = omega * r
            retval = self.heuristic_helper(euc_distance=d_v, angle=math.pi / 2)
            return retval if retval == very_big_number_ohno else retval + l_s
        else:
            # the pivot point
            if angle == math.pi / 2:
                # save the calculation time for the special case (would work anyway though)
                y_r = 0
                x_r = r
            else:
                y_r = -r / math.sqrt(1 + math.tan(theta) ** 2)
                x_r = -y_r * math.tan(theta)
            if d <= 2 * x_r:
                # if we can't hit the center of the target
                # draw the inscribed circle and see if we can hit that instead
                # if we can hit the inscribed circle, then we can guarantee hitting the target
                # however, the heuristic distance likely overestimates true distance slightly in this case
                # in truth, the length of the path that is inside the target is not accounted for in any algorithm
                # of those here, including the given DFS
                # and so the ABSOLUTE minimum path can never be guaranteed
                goal_center = self.get_goal_information()
                r_inscribed = min(goal_center[2], goal_center[3])
                d = d + r_inscribed
                if d <= 2 * x_r:
                    # We can't turn in time to hit the target!
                    return very_big_number_ohno
                # intermediate variables
            var_a = x_r ** 2 - 2 * x_r * d
            var_b = y_r ** 2 - r ** 2 + d ** 2
            var_c = r * math.sqrt(var_a + var_b)
            # the tangent point and slope
            x_t = ((x_r ** 3 + var_c * y_r) - (2 * x_r ** 2) * d + x_r * var_b + (r ** 2) * d) / (
                    var_a + var_b + r ** 2)
            a = -(var_c - y_r * (x_r - d)) / (var_a + var_b - y_r ** 2)
            y_t = a * (x_t - d)
            # the lengths of the straight line and arc respectively
            l_1 = math.sqrt((x_t - d) ** 2 + y_t ** 2)
            l_2 = theta * r * (theta - math.atan(a)) / math.asin(x_r / r)
            # return with the sum length
            return l_1 + l_2
            # I know this looks a little scary, but I have tested it in Matlab and Geogebra, and it's correct

    def evaluation_function(self, node_current):
        """
        f(x) = g(x) + w * h(x)
        """
        #w = 2
        g = self.cost_function(node_current)
        h = self.heuristic_function_2(node_current)
        f = g + (self.w * h)
        return f

    def print_results(self, goal_node, initial_node):
        f = open("output.txt", "a")
        #visited_nodes_number = len(self.get_node_path(goal_node))
        path_nodes_num = len(self.get_node_path(goal_node))
        visited_nodes_number = self.visited_nodes_num

        print("\tVisited Nodes number: " + str(visited_nodes_number) + "")
        f.write("\tVisited Nodes number: " + str(visited_nodes_number) + "\n")
        print("\tPath:", end="")
        f.write("\tPath:")
        for i in range(int(path_nodes_num)):
            print("(" + str(self.get_node_path(goal_node)[i][0]) + "," + str(self.get_node_path(goal_node)[i][1]) + ")",
                  end="")
            f.write("(" + str(self.get_node_path(goal_node)[i][0]) + "," + str(self.get_node_path(goal_node)[i][1]) + ")"
                  )
            if i < path_nodes_num - 1:
                print("->", end="")
                f.write("->")
            else:
                print("")
                f.write("\n")
        print("\tHeuristic Cost (initial node):" + str(self.heuristic_function_2(initial_node)))
        f.write("\tHeuristic Cost (initial node):" + str(self.heuristic_function_2(initial_node))+"\n")
        print("\tEstimated Cost:" + str(self.heuristic_function_2(initial_node)))
        f.write("\tEstimated Cost:" + str(self.heuristic_function_2(initial_node))+"\n")
        print("\tReal Cost:" + str(self.cost_function(goal_node)))
        f.write("\tReal Cost:" + str(self.cost_function(goal_node)) + "\n")

        f.close()


    def loopy_astar(self, node_current):
        # list of nodes to be explored
        open_list = [node_current]
        # list of fcosts of nodes in open (dynamic programming)
        open_f = [self.evaluation_function(node_current)]
        # list of nodes already explored
        closed_list = []

        while bool(open_list):

            # open_list and open_f are sorted by fcost
            next_index = 0
            # resolve a 2-way fcost tie by hcost
            # due to continuous nature of problem, it is near impossible that a more than 2-way tie occurs
            if len(open_list) > 1 and open_f[0] == open_f[1] \
                    and self.heuristic_function_2(open_list[1]) < self.heuristic_function_2(open_list[0]):
                next_index = 1
            # pop the node in open with t lowest fcost
            next_node = open_list.pop(next_index)
            # also pop from the stored fcosts to keep lists in sync
            open_f.pop(next_index)
            closed_list.append(next_node)

            # examine all the potential successors of explored node
            for primitive_successor in next_node.get_successors():
                self.visited_nodes_num = self.visited_nodes_num + 1
                collision_flag, successor = self.take_step(successor=primitive_successor, node_current=next_node)

                # if it collides with an obstacle or boundary skip this successor
                if collision_flag:
                    continue
                # skip successor if it has already been explored
                if successor in closed_list:
                    continue
                # check whether goal is reached in move from node to successor
                goal_flag = self.goal_reached(successor=primitive_successor,
                                              node_current=next_node)
                # if goal is reached, return with the solution path
                if goal_flag:
                    initial_node = closed_list.pop(0)
                    self.print_results(successor, initial_node)
                    return True

                # evaluate successor
                f_successor = self.evaluation_function(successor)

                # if there is another path to successor, choose the better one
                if successor in open_list:
                    index_contender = open_list.index(successor)
                    f_contender = open_f[index_contender]
                    if f_contender <= f_successor:
                        # skip successor (there is a better path to it)
                        continue
                    # update node in open with better path
                    open_list[index_contender] = successor
                    open_f[index_contender] = f_successor
                    # sort open and fcost by fcost (courtesy of stackoverflow.com)
                    open_list = [x for _, x in sorted(zip(open_f, open_list), key=lambda pair: pair[0])]
                    open_f.sort()
                    continue
                open_list.append(successor)
                open_f.append(f_successor)
                # sort open and fcost by fcost
                open_list = [x for _, x in sorted(zip(open_f, open_list), key=lambda pair: pair[0])]
                open_f.sort()

    def execute_search(self, w,time_pause) -> Tuple[
        Union[None, List[List[State]]], Union[None, List[MotionPrimitive]], Any]:
        node_initial = self.initialize_search(time_pause=time_pause)
        print(self.get_obstacles_information())
        print(self.get_goal_information())
        print(self.get_node_information(node_initial))
        self.w=w
        """Enter your code here"""
        self.loopy_astar(node_initial)

        return True


class Astar(SequentialSearch):
    """
    Class for Astar Search algorithm.
    """

    def __init__(self, scenario, planningProblem, automaton, plot_config=DefaultPlotConfig):
        super().__init__(scenario=scenario, planningProblem=planningProblem, automaton=automaton,
                         plot_config=plot_config)
