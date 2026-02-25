/**
 * Team Management API Routes
 * 
 * ADMIN ONLY:
 * - Add new team members
 * - Edit team member details
 * - Assign members to projects
 * - Manage roles and permissions
 * - Remove team members
 */

import express from 'express';
import User from '../models/User.js';

const router = express.Router();

/**
 * Get all team members
 * GET /api/team/members
 * ADMIN ONLY
 */
router.get('/members', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const members = await User.find({}, '-password').sort({ createdAt: -1 });
    
    res.json({
      success: true,
      members: members.map(member => ({
        _id: member._id,
        name: member.name,
        email: member.email,
        role: member.role,
        githubUsername: member.githubUsername || '',
        assignedProjects: member.assignedProjects || [],
        status: member.status || 'active',
        joinedDate: member.createdAt
      }))
    });
  } catch (error) {
    console.error('Error fetching team members:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to fetch team members' 
    });
  }
});

/**
 * Add new team member
 * POST /api/team/members
 * ADMIN ONLY
 */
router.post('/members', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { name, email, role, githubUsername, assignedProjects, status } = req.body;

    // Check if user already exists
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(400).json({ 
        success: false, 
        error: 'User with this email already exists' 
      });
    }

    // Create new user with default password
    const defaultPassword = 'password123'; // User should change this on first login
    
    const newMember = new User({
      name,
      email,
      password: defaultPassword, // Will be hashed by User model pre-save hook
      role: role || 'contributor',
      githubUsername: githubUsername || '',
      assignedProjects: assignedProjects || [],
      status: status || 'active'
    });

    await newMember.save();

    res.json({
      success: true,
      message: 'Team member added successfully',
      member: {
        _id: newMember._id,
        name: newMember.name,
        email: newMember.email,
        role: newMember.role,
        githubUsername: newMember.githubUsername,
        assignedProjects: newMember.assignedProjects,
        status: newMember.status
      }
    });
  } catch (error) {
    console.error('Error adding team member:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to add team member' 
    });
  }
});

/**
 * Update team member
 * PUT /api/team/members/:id
 * ADMIN ONLY
 */
router.put('/members/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;
    const { name, email, role, githubUsername, assignedProjects, status } = req.body;

    const member = await User.findById(id);
    if (!member) {
      return res.status(404).json({ 
        success: false, 
        error: 'Team member not found' 
      });
    }

    // Check if email is being changed and if it's already taken
    if (email !== member.email) {
      const existingUser = await User.findOne({ email });
      if (existingUser) {
        return res.status(400).json({ 
          success: false, 
          error: 'Email already in use by another user' 
        });
      }
    }

    // Update fields
    member.name = name || member.name;
    member.email = email || member.email;
    member.role = role || member.role;
    member.githubUsername = githubUsername !== undefined ? githubUsername : member.githubUsername;
    member.assignedProjects = assignedProjects !== undefined ? assignedProjects : member.assignedProjects;
    member.status = status || member.status;

    await member.save();

    res.json({
      success: true,
      message: 'Team member updated successfully',
      member: {
        _id: member._id,
        name: member.name,
        email: member.email,
        role: member.role,
        githubUsername: member.githubUsername,
        assignedProjects: member.assignedProjects,
        status: member.status
      }
    });
  } catch (error) {
    console.error('Error updating team member:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to update team member' 
    });
  }
});

/**
 * Delete team member
 * DELETE /api/team/members/:id
 * ADMIN ONLY
 */
router.delete('/members/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;

    const member = await User.findById(id);
    if (!member) {
      return res.status(404).json({ 
        success: false, 
        error: 'Team member not found' 
      });
    }

    await User.findByIdAndDelete(id);

    res.json({
      success: true,
      message: 'Team member removed successfully'
    });
  } catch (error) {
    console.error('Error deleting team member:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to remove team member' 
    });
  }
});

/**
 * Get team member by ID
 * GET /api/team/members/:id
 * ADMIN ONLY
 */
router.get('/members/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;
    const member = await User.findById(id, '-password');

    if (!member) {
      return res.status(404).json({ 
        success: false, 
        error: 'Team member not found' 
      });
    }

    res.json({
      success: true,
      member: {
        _id: member._id,
        name: member.name,
        email: member.email,
        role: member.role,
        githubUsername: member.githubUsername,
        assignedProjects: member.assignedProjects,
        status: member.status,
        joinedDate: member.createdAt
      }
    });
  } catch (error) {
    console.error('Error fetching team member:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to fetch team member' 
    });
  }
});

export default router;
