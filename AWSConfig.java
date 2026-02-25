package in.abhinay.billingsoftware.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AWSConfig {
    @Value("${aws.access.key}")
    private String accessKey;
    @Value("${aws.secret.key}")
    private String secretKey;
    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }

}
/**
 * Project Management API Routes
 * 
 * ADMIN ONLY:
 * - Create new projects
 * - Edit project details
 * - Assign team members to projects
 * - Delete projects
 */

import express from 'express';
import Project from '../models/Project.js';
import User from '../models/User.js';

const router = express.Router();

/**
 * Get all projects
 * GET /api/projects
 * ADMIN ONLY
 */
router.get('/', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const projects = await Project.find()
      .populate('teamMembers', 'name email githubUsername')
      .sort({ createdAt: -1 });
    
    res.json({
      success: true,
      projects: projects.map(project => ({
        _id: project._id,
        projectName: project.projectName,
        description: project.description,
        githubRepo: project.githubRepo,
        teamMembers: project.teamMembers,
        status: project.status,
        createdAt: project.createdAt,
        updatedAt: project.updatedAt
      }))
    });
  } catch (error) {
    console.error('Error fetching projects:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to fetch projects' 
    });
  }
});

/**
 * Create new project
 * POST /api/projects
 * ADMIN ONLY
 */
router.post('/', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { projectName, description, githubRepo, teamMembers, status } = req.body;

    if (!projectName || !githubRepo) {
      return res.status(400).json({ 
        success: false, 
        error: 'Project name and GitHub repository are required' 
      });
    }

    // Verify team members exist
    if (teamMembers && teamMembers.length > 0) {
      const validMembers = await User.find({ _id: { $in: teamMembers } });
      if (validMembers.length !== teamMembers.length) {
        return res.status(400).json({ 
          success: false, 
          error: 'One or more team members not found' 
        });
      }
    }

    const newProject = new Project({
      projectName,
      description: description || '',
      githubRepo,
      teamMembers: teamMembers || [],
      status: status || 'active'
    });

    await newProject.save();

    // Populate team members for response
    await newProject.populate('teamMembers', 'name email githubUsername');

    res.json({
      success: true,
      message: 'Project created successfully',
      project: {
        _id: newProject._id,
        projectName: newProject.projectName,
        description: newProject.description,
        githubRepo: newProject.githubRepo,
        teamMembers: newProject.teamMembers,
        status: newProject.status,
        createdAt: newProject.createdAt
      }
    });
  } catch (error) {
    console.error('Error creating project:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to create project' 
    });
  }
});

/**
 * Update project
 * PUT /api/projects/:id
 * ADMIN ONLY
 */
router.put('/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;
    const { projectName, description, githubRepo, teamMembers, status } = req.body;

    const project = await Project.findById(id);
    if (!project) {
      return res.status(404).json({ 
        success: false, 
        error: 'Project not found' 
      });
    }

    // Verify team members exist if provided
    if (teamMembers && teamMembers.length > 0) {
      const validMembers = await User.find({ _id: { $in: teamMembers } });
      if (validMembers.length !== teamMembers.length) {
        return res.status(400).json({ 
          success: false, 
          error: 'One or more team members not found' 
        });
      }
    }

    // Update fields
    if (projectName) project.projectName = projectName;
    if (description !== undefined) project.description = description;
    if (githubRepo) project.githubRepo = githubRepo;
    if (teamMembers !== undefined) project.teamMembers = teamMembers;
    if (status) project.status = status;

    await project.save();
    await project.populate('teamMembers', 'name email githubUsername');

    res.json({
      success: true,
      message: 'Project updated successfully',
      project: {
        _id: project._id,
        projectName: project.projectName,
        description: project.description,
        githubRepo: project.githubRepo,
        teamMembers: project.teamMembers,
        status: project.status,
        updatedAt: project.updatedAt
      }
    });
  } catch (error) {
    console.error('Error updating project:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to update project' 
    });
  }
});

/**
 * Delete project
 * DELETE /api/projects/:id
 * ADMIN ONLY
 */
router.delete('/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;

    const project = await Project.findById(id);
    if (!project) {
      return res.status(404).json({ 
        success: false, 
        error: 'Project not found' 
      });
    }

    await Project.findByIdAndDelete(id);

    res.json({
      success: true,
      message: 'Project deleted successfully'
    });
  } catch (error) {
    console.error('Error deleting project:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to delete project' 
    });
  }
});

/**
 * Get project by ID
 * GET /api/projects/:id
 * ADMIN ONLY
 */
router.get('/:id', async (req, res) => {
  try {
    const userRole = req.headers['user-role'];
    
    if (userRole !== 'admin') {
      return res.status(403).json({ 
        success: false, 
        error: 'Access denied. Admin only.' 
      });
    }

    const { id } = req.params;
    const project = await Project.findById(id)
      .populate('teamMembers', 'name email githubUsername');

    if (!project) {
      return res.status(404).json({ 
        success: false, 
        error: 'Project not found' 
      });
    }

    res.json({
      success: true,
      project: {
        _id: project._id,
        projectName: project.projectName,
        description: project.description,
        githubRepo: project.githubRepo,
        teamMembers: project.teamMembers,
        status: project.status,
        createdAt: project.createdAt,
        updatedAt: project.updatedAt
      }
    });
  } catch (error) {
    console.error('Error fetching project:', error);
    res.status(500).json({ 
      success: false, 
      error: 'Failed to fetch project' 
    });
  }
});

export default router;

